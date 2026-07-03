package com.backend.service;

import com.backend.dto.auth.AuthResponse;
import com.backend.dto.auth.ForgotPasswordRequest;
import com.backend.dto.auth.LoginRequest;
import com.backend.dto.auth.RegisterRequest;
import com.backend.dto.auth.ResetPasswordRequest;
import com.backend.model.AreaCode;
import com.backend.model.PasswordResetToken;
import com.backend.model.Role;
import com.backend.model.User;
import com.backend.repository.PasswordResetTokenRepository;
import com.backend.repository.UserRepository;
import com.backend.security.JwtService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final int RESET_TOKEN_BYTES = 32;
    private static final int RESET_TOKEN_EXPIRATION_MINUTES = 30;

    private static final String INVALID_CREDENTIALS_MESSAGE = "Usuario o contraseña inválidos.";
    private static final String INACTIVE_USER_MESSAGE =
            "Tu usuario está inhabilitado. Comunícate con tu jefe inmediato o con el administrador del sistema.";
    private static final String EXPIRED_USER_MESSAGE =
            "La vigencia de tu usuario finalizó. Comunícate con tu jefe inmediato o con el administrador del sistema.";
    private static final String NOT_STARTED_USER_MESSAGE =
            "Tu acceso todavía no está habilitado porque la vigencia no ha iniciado.";

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JavaMailSender mailSender;
    private final UserAccessEvaluator userAccessEvaluator;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${spring.mail.username:}")
    private String from;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            PasswordResetTokenRepository passwordResetTokenRepository,
            JavaMailSender mailSender,
            UserAccessEvaluator userAccessEvaluator
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.mailSender = mailSender;
        this.userAccessEvaluator = userAccessEvaluator;
    }

    public AuthResponse login(LoginRequest request) {
        String principal = request.resolvePrincipal();

        if (principal.isBlank()) {
            throw new IllegalArgumentException("Debes ingresar usuario o correo.");
        }

        User user = userRepository.findByUsername(principal)
                .or(() -> userRepository.findByEmail(principal))
                .orElseThrow(() -> new IllegalArgumentException(INVALID_CREDENTIALS_MESSAGE));

        if (!user.isEnabled()) {
            throw new IllegalArgumentException(INACTIVE_USER_MESSAGE);
        }

        if (isExpired(user)) {
            throw new IllegalArgumentException(EXPIRED_USER_MESSAGE);
        }
        if (isNotStarted(user)) {
            throw new IllegalArgumentException(NOT_STARTED_USER_MESSAGE);
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        request.getPassword()
                )
        );

        user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException(INVALID_CREDENTIALS_MESSAGE));

        if (!user.isEnabled()) {
            throw new IllegalArgumentException(INACTIVE_USER_MESSAGE);
        }

        if (isExpired(user)) {
            throw new IllegalArgumentException(EXPIRED_USER_MESSAGE);
        }
        if (isNotStarted(user)) {
            throw new IllegalArgumentException(NOT_STARTED_USER_MESSAGE);
        }

        Set<Role> effectiveRoles = userAccessEvaluator.effectiveRoles(user);

        if (effectiveRoles.isEmpty()) {
            throw new IllegalArgumentException("Tu usuario no tiene roles vigentes. Comunícate con el administrador del sistema.");
        }

        Set<AreaCode> effectiveAllowedAreas = userAccessEvaluator.effectiveAllowedAreas(user);

        Set<String> authorities = effectiveRoles
                .stream()
                .map(role -> "ROLE_" + role.name())
                .collect(Collectors.toSet());

        String token = jwtService.generateToken(user.getUsername(), authorities);

        return new AuthResponse(
                token,
                user.getUsername(),
                user.getEmail(),
                effectiveRoles,
                effectiveAllowedAreas,
                user.isGlobalAreaAccess()
        );
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario ya existe.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El correo ya existe.");
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(new HashSet<>(Set.of(Role.OPERADOR)));
        user.setAllowedAreas(
                request.getAllowedAreas() == null
                        ? new HashSet<>()
                        : new HashSet<>(request.getAllowedAreas())
        );
        user.setEnabled(true);

        User saved = userRepository.save(user);

        Set<Role> effectiveRoles = userAccessEvaluator.effectiveRoles(saved);
        Set<AreaCode> effectiveAllowedAreas = userAccessEvaluator.effectiveAllowedAreas(saved);

        Set<String> authorities = effectiveRoles
                .stream()
                .map(role -> "ROLE_" + role.name())
                .collect(Collectors.toSet());

        String token = jwtService.generateToken(saved.getUsername(), authorities);

        return new AuthResponse(
                token,
                saved.getUsername(),
                saved.getEmail(),
                effectiveRoles,
                effectiveAllowedAreas,
                saved.isGlobalAreaAccess()
        );
    }

    public void requestPasswordReset(ForgotPasswordRequest request) {
        String email = safe(request.getEmail()).toLowerCase();

        /*
         * Seguridad:
         * Si el correo no existe, no se lanza error.
         * Así no se filtra qué usuarios están registrados.
         */
        userRepository.findByEmail(email).ifPresent(user -> {
            if (!user.isEnabled()) {
                return;
            }

            invalidatePreviousResetTokens(user.getId());

            String rawToken = generateRawToken();
            String tokenHash = hashToken(rawToken);

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUserId(user.getId());
            resetToken.setTokenHash(tokenHash);
            resetToken.setCreatedAt(LocalDateTime.now());
            resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRATION_MINUTES));
            resetToken.setUsed(false);
            resetToken.setUsedAt(null);

            passwordResetTokenRepository.save(resetToken);

            sendPasswordResetEmail(user, rawToken);
        });
    }

    public void resetPassword(ResetPasswordRequest request) {
        String rawToken = safe(request.getToken());

        if (rawToken.isBlank()) {
            throw new IllegalArgumentException("Token inválido.");
        }

        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenHashAndUsedFalse(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("El enlace de recuperación no es válido o ya fue usado."));

        if (resetToken.getExpiresAt() == null || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            resetToken.setUsed(true);
            resetToken.setUsedAt(LocalDateTime.now());
            passwordResetTokenRepository.save(resetToken);

            throw new IllegalArgumentException("El enlace de recuperación expiró. Solicita uno nuevo.");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        if (!user.isEnabled()) {
            throw new IllegalArgumentException("El usuario está deshabilitado.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);

        invalidatePreviousResetTokens(user.getId());
    }

    private void invalidatePreviousResetTokens(String userId) {
        List<PasswordResetToken> activeTokens = passwordResetTokenRepository.findByUserIdAndUsedFalse(userId);

        activeTokens.forEach(token -> {
            token.setUsed(true);
            token.setUsedAt(LocalDateTime.now());
        });

        if (!activeTokens.isEmpty()) {
            passwordResetTokenRepository.saveAll(activeTokens);
        }
    }

    private String generateRawToken() {
        byte[] bytes = new byte[RESET_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el token de recuperación.", ex);
        }
    }

    private void sendPasswordResetEmail(User user, String rawToken) {
        String resetUrl = buildResetUrl(rawToken);
        String subject = "Recuperación de contraseña - SSTAlturas";
        String body = buildPasswordResetBody(user, resetUrl);

        try {
            sendHtmlEmail(user.getEmail(), subject, body);
        } catch (MailException | MessagingException ex) {
            throw new IllegalStateException("No se pudo enviar el correo de recuperación.", ex);
        }
    }

    private void sendHtmlEmail(String to, String subject, String body) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        if (!safe(from).isBlank()) {
            helper.setFrom(from);
        }

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, true);

        mailSender.send(message);
    }

    private String buildResetUrl(String rawToken) {
        String baseUrl = safe(frontendBaseUrl);

        if (baseUrl.isBlank() || isOutdatedFrontendUrl(baseUrl)) {
            baseUrl = "https://sstalturas.ebsa.com.co";
        }

        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        return baseUrl + "/reset-password?token=" + rawToken;
    }

    private String buildPasswordResetBody(User user, String resetUrl) {
        String username = safe(user.getUsername()).isBlank()
                ? "usuario"
                : safe(user.getUsername());

        return """
                <!doctype html>
                <html>
                <body style="margin:0; padding:0; background:#f8fafc; font-family:Arial, Helvetica, sans-serif; color:#111827;">
                  <div style="max-width:620px; margin:0 auto; padding:24px;">
                    <div style="background:#ffffff; border:1px solid #e5e7eb; border-radius:16px; padding:24px;">
                      <h1 style="margin:0 0 12px; font-size:20px; color:#111827;">
                        Recuperación de contraseña
                      </h1>

                      <p style="margin:0 0 14px; font-size:14px; line-height:1.5;">
                        Hola, <strong>%s</strong>.
                      </p>

                      <p style="margin:0 0 18px; font-size:14px; line-height:1.5;">
                        Recibimos una solicitud para restablecer la contraseña de tu cuenta en SSTAlturas.
                      </p>

                      <p style="margin:0 0 22px; font-size:14px; line-height:1.5;">
                        Para crear una nueva contraseña, haz clic en el siguiente botón:
                      </p>

                      <p style="margin:0 0 22px;">
                        <a href="%s"
                           style="display:inline-block; padding:12px 18px; border-radius:10px; background:#16a34a; color:#ffffff; text-decoration:none; font-size:14px; font-weight:700;">
                          Restablecer contraseña
                        </a>
                      </p>

                      <p style="margin:0 0 10px; font-size:13px; line-height:1.5; color:#475569;">
                        Este enlace vence en 30 minutos y solo puede usarse una vez.
                      </p>

                      <p style="margin:0; font-size:13px; line-height:1.5; color:#475569;">
                        Si no solicitaste este cambio, puedes ignorar este mensaje.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(escapeHtml(username), escapeHtml(resetUrl));
    }

    private String escapeHtml(String value) {
        return safe(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isOutdatedFrontendUrl(String value) {
        String normalized = safe(value).toLowerCase();
        return normalized.contains("localhost") ||
                normalized.contains("127.0.0.1") ||
                normalized.contains("0.0.0.0") ||
                normalized.contains("ssralturas.ebsa.com.co");
    }

    private boolean isExpired(User user) {
        return user.getAccountExpirationDate() != null &&
                !LocalDateTime.now().isBefore(user.getAccountExpirationDate());
    }

    private boolean isNotStarted(User user) {
        return user.getAccountStartDate() != null &&
                LocalDateTime.now().isBefore(user.getAccountStartDate());
    }
}
