package com.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/actuator/health",
                                "/",
                                "/index.html",
                                "/assets/**",
                                "/favicon.ico"
                        ).permitAll()

                        /*
                         * Administración:
                         * VISUALIZADOR no puede administrar usuarios, correos ni configuración.
                         */
                        .requestMatchers("/api/users/**", "/api/audit-logs/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/api/settings/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN", "IT", "OPERADOR")

                        /*
                         * Carga documental:
                         * VISUALIZADOR no puede cargar PDFs.
                         */
                        .requestMatchers(HttpMethod.POST, "/api/documents/upload", "/api/documents/upload/batch-auto")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN", "OPERADOR")

                        /*
                         * Reanálisis:
                         * VISUALIZADOR no puede reanalizar documentos.
                         */
                        .requestMatchers(HttpMethod.GET, "/api/documents/*/analyze")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN", "OPERADOR")

                        /*
                         * PDF original:
                         * VISUALIZADOR no puede abrir soportes PDF por privacidad.
                         */
                        .requestMatchers(HttpMethod.GET, "/api/documents/*/view")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN", "OPERADOR", "APROBADOR")

                        .requestMatchers(HttpMethod.POST, "/api/documents/backfill-extracted-data")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")

                        /*
                         * Eliminación documental:
                         * Solo SUPER_ADMIN.
                         */
                        .requestMatchers(HttpMethod.DELETE, "/api/documents/historical/issues", "/api/documents/historical/issues/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN", "OPERADOR")

                        .requestMatchers(HttpMethod.DELETE, "/api/documents/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN", "OPERADOR")

                        /*
                         * Revisión, aprobación y reenvío:
                         * VISUALIZADOR no puede aprobar, rechazar ni reenviar correo.
                         */
                        .requestMatchers(HttpMethod.POST, "/api/documents/*/approve", "/api/documents/*/reject", "/api/documents/*/resend-email")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN", "APROBADOR")

                        .requestMatchers(HttpMethod.POST, "/api/documents/approve-bulk", "/api/documents/reject-bulk")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN", "APROBADOR")

                        /*
                         * Reporte PDF individual:
                         * No se usa para visualizador.
                         */
                        .requestMatchers(HttpMethod.GET, "/api/documents/*/report")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN", "APROBADOR")

                        /*
                         * CSV de cargue masivo:
                         * VISUALIZADOR no puede descargar CSV.
                         */
                        .requestMatchers(HttpMethod.GET, "/api/reports/ministry-csv", "/api/reports/workers-csv", "/api/reports/ministry/csv")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN", "OPERADOR")

                        /*
                         * Lotes:
                         * Solo revisión / administración.
                         */
                        .requestMatchers("/api/document-batches/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN", "APROBADOR")

                        /*
                         * Trabajadores:
                         * VISUALIZADOR solo puede leer.
                         */
                        .requestMatchers(HttpMethod.POST, "/api/employees/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN", "OPERADOR")

                        .requestMatchers(HttpMethod.PUT, "/api/employees/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN", "OPERADOR")

                        .requestMatchers(HttpMethod.DELETE, "/api/employees/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN", "OPERADOR")

                        /*
                         * Constancias:
                         * VISUALIZADOR solo puede consultar/listar.
                         */
                        .requestMatchers(HttpMethod.POST, "/api/employees/*/certificates")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN", "OPERADOR")

                        .requestMatchers(HttpMethod.DELETE, "/api/certificates/**")
                        .hasAnyRole("ADMIN", "SUPER_ADMIN")

                        /*
                         * El resto de consultas autenticadas quedan permitidas,
                         * pero los servicios siguen filtrando por zona.
                         */
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
