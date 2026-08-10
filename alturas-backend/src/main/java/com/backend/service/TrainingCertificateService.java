package com.backend.service;

import com.backend.model.AreaCode;
import com.backend.model.Employee;
import com.backend.model.ManagedDocument;
import com.backend.model.TrainingCertificate;
import com.backend.model.User;
import com.backend.repository.EmployeeRepository;
import com.backend.repository.ManagedDocumentRepository;
import com.backend.repository.TrainingCertificateRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class TrainingCertificateService {

    private static final String UPLOAD_DIR = "uploads/training-certificates";
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String DELETED_STATUS = "DELETED";

    private static final String REVIEW_APPROVED = "APPROVED";
    private static final String NOTIFICATION_SENT = "SENT";

    private final TrainingCertificateRepository trainingCertificateRepository;
    private final EmployeeRepository employeeRepository;
    private final ManagedDocumentRepository managedDocumentRepository;
    private final AccessScopeService accessScopeService;

    public TrainingCertificateService(
            TrainingCertificateRepository trainingCertificateRepository,
            EmployeeRepository employeeRepository,
            ManagedDocumentRepository managedDocumentRepository,
            AccessScopeService accessScopeService
    ) {
        this.trainingCertificateRepository = trainingCertificateRepository;
        this.employeeRepository = employeeRepository;
        this.managedDocumentRepository = managedDocumentRepository;
        this.accessScopeService = accessScopeService;
    }

    public Map<String, Object> uploadCertificate(String employeeId, MultipartFile file) {
        Employee employee = getEmployee(employeeId);
        assertCanAccessEmployee(employee);
        accessScopeService.assertCanWriteEmployees();
        validateCertificateEligibility(employeeId);
        validateFile(file);

        User currentUser = accessScopeService.getCurrentUser();

        try {
            String originalFileName = sanitizeOriginalFileName(file.getOriginalFilename());
            String extension = resolveExtension(originalFileName);
            String storedFileName = UUID.randomUUID() + extension;

            Path employeeFolder = Paths.get(UPLOAD_DIR, employeeId)
                    .toAbsolutePath()
                    .normalize();

            Files.createDirectories(employeeFolder);

            Path targetPath = employeeFolder.resolve(storedFileName).normalize();

            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            TrainingCertificate certificate = new TrainingCertificate();
            certificate.setEmployeeId(employeeId);
            certificate.setOriginalFileName(originalFileName);
            certificate.setStoredFileName(storedFileName);
            certificate.setContentType(file.getContentType());
            certificate.setSizeBytes(file.getSize());
            certificate.setFilePath(targetPath.toString());
            certificate.setUploadedBy(currentUser != null ? currentUser.getUsername() : "system");
            certificate.setUploadedAt(LocalDateTime.now());
            certificate.setStatus(ACTIVE_STATUS);

            TrainingCertificate saved = trainingCertificateRepository.save(certificate);

            return toResponse(saved);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo cargar la constancia.", ex);
        }
    }

    public Map<String, Object> getCertificateEligibility(String employeeId) {
        Employee employee = getEmployee(employeeId);
        assertCanAccessEmployee(employee);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("employeeId", employeeId);

        Optional<ManagedDocument> validDocument = findLatestApprovedAndNotifiedDocument(employeeId);

        if (validDocument.isPresent()) {
            ManagedDocument document = validDocument.get();

            response.put("eligible", true);
            response.put("message", "El trabajador tiene una evaluación aprobada y notificada correctamente.");
            response.put("documentId", document.getId());
            response.put("reviewStatus", document.getReviewStatus());
            response.put("notificationStatus", document.getNotificationStatus());
            response.put("reviewedAt", document.getReviewedAt());
            response.put("notifiedAt", document.getNotifiedAt());

            return response;
        }

        Optional<ManagedDocument> latestDocument = findLatestDocument(employeeId);

        response.put("eligible", true);
        response.put("message", "La carga de constancia está habilitada independientemente del estado de la evaluación (por solicitud).");

        latestDocument.ifPresent(document -> {
            response.put("documentId", document.getId());
            response.put("reviewStatus", document.getReviewStatus());
            response.put("notificationStatus", document.getNotificationStatus());
            response.put("notificationError", document.getNotificationError());
            response.put("reviewedAt", document.getReviewedAt());
            response.put("notifiedAt", document.getNotifiedAt());
        });

        return response;
    }

    public List<Map<String, Object>> listCertificates(String employeeId) {
        Employee employee = getEmployee(employeeId);
        assertCanAccessEmployee(employee);

        return trainingCertificateRepository
                .findByEmployeeIdAndStatusOrderByUploadedAtDesc(employeeId, ACTIVE_STATUS)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TrainingCertificate getCertificateForDownload(String certificateId) {
        TrainingCertificate certificate = trainingCertificateRepository.findById(certificateId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la constancia."));

        if (!ACTIVE_STATUS.equalsIgnoreCase(safe(certificate.getStatus()))) {
            throw new IllegalArgumentException("La constancia no está disponible.");
        }

        Employee employee = getEmployee(certificate.getEmployeeId());
        assertCanAccessEmployee(employee);

        return certificate;
    }

    public Resource loadCertificateFile(TrainingCertificate certificate) {
        try {
            Path path = Paths.get(certificate.getFilePath())
                    .toAbsolutePath()
                    .normalize();

            if (!Files.exists(path)) {
                throw new IllegalArgumentException("No se encontró el archivo físico de la constancia.");
            }

            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalArgumentException("No se pudo leer el archivo de la constancia.");
            }

            return resource;
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("Ruta de archivo inválida.", ex);
        }
    }

    public void deleteCertificate(String certificateId) {
        TrainingCertificate certificate = trainingCertificateRepository.findById(certificateId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la constancia."));

        Employee employee = getEmployee(certificate.getEmployeeId());
        assertCanAccessEmployee(employee);

        certificate.setStatus(DELETED_STATUS);
        trainingCertificateRepository.save(certificate);
    }

    private void validateCertificateEligibility(String employeeId) {
        // Validation removed as per user request to allow uploading
        // Constancias regardless of whether the worker is fully qualified.
    }

    private Optional<ManagedDocument> findLatestApprovedAndNotifiedDocument(String employeeId) {
        return managedDocumentRepository
                .findByEmployeeIdOrderByUploadedAtDesc(employeeId)
                .stream()
                .filter(this::isApprovedAndNotifiedCorrectly)
                .findFirst();
    }

    private Optional<ManagedDocument> findLatestDocument(String employeeId) {
        return managedDocumentRepository
                .findByEmployeeIdOrderByUploadedAtDesc(employeeId)
                .stream()
                .findFirst();
    }

    private boolean isApprovedAndNotifiedCorrectly(ManagedDocument document) {
        if (document == null) {
            return false;
        }

        boolean approved = REVIEW_APPROVED.equalsIgnoreCase(safe(document.getReviewStatus()));
        boolean sent = NOTIFICATION_SENT.equalsIgnoreCase(safe(document.getNotificationStatus()));
        boolean withoutNotificationError = safe(document.getNotificationError()).isBlank();

        return approved && sent && withoutNotificationError;
    }

    private String buildEligibilityMessage(ManagedDocument latestDocument) {
        if (latestDocument == null) {
            return "La carga de constancia se habilita cuando el trabajador tenga una evaluación aprobada y notificada correctamente.";
        }

        String reviewStatus = safe(latestDocument.getReviewStatus());
        String notificationStatus = safe(latestDocument.getNotificationStatus());
        String notificationError = safe(latestDocument.getNotificationError());

        if (!REVIEW_APPROVED.equalsIgnoreCase(reviewStatus)) {
            return "No se puede cargar la constancia porque la evaluación del trabajador todavía no está aprobada.";
        }

        if (!NOTIFICATION_SENT.equalsIgnoreCase(notificationStatus)) {
            return "No se puede cargar la constancia porque todavía no se ha enviado correctamente el correo al trabajador.";
        }

        if (!notificationError.isBlank()) {
            return "No se puede cargar la constancia porque la notificación del trabajador registra errores.";
        }

        return "La carga de constancia se habilita cuando el trabajador tenga una evaluación aprobada y notificada correctamente.";
    }

    private Employee getEmployee(String employeeId) {
        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException("Debe indicar el trabajador.");
        }

        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el trabajador."));
    }

    private void assertCanAccessEmployee(Employee employee) {
        User currentUser = accessScopeService.getCurrentUser();

        if (accessScopeService.hasGlobalDocumentAccess(currentUser)) {
            return;
        }

        AreaCode employeeArea = employee.getAreaCode();

        if (employeeArea == null) {
            throw new IllegalArgumentException("No tiene permisos para consultar este trabajador.");
        }

        if (!accessScopeService.getAllowedAreas(currentUser).contains(employeeArea)) {
            throw new IllegalArgumentException("No tiene permisos para consultar este trabajador.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar un archivo.");
        }

        String originalFileName = safe(file.getOriginalFilename()).toLowerCase();

        if (!originalFileName.endsWith(".pdf")) {
            throw new IllegalArgumentException("Solo se permite cargar constancias en formato PDF.");
        }
    }

    private String sanitizeOriginalFileName(String value) {
        String fileName = safe(value);

        if (fileName.isBlank()) {
            return "constancia.pdf";
        }

        return fileName
                .replace("\\", "_")
                .replace("/", "_")
                .replace("\n", "_")
                .replace("\r", "_")
                .trim();
    }

    private String resolveExtension(String originalFileName) {
        String value = safe(originalFileName);
        int dotIndex = value.lastIndexOf('.');

        if (dotIndex >= 0 && dotIndex < value.length() - 1) {
            return value.substring(dotIndex).toLowerCase();
        }

        return ".pdf";
    }

    private Map<String, Object> toResponse(TrainingCertificate certificate) {
        Map<String, Object> response = new LinkedHashMap<>();

        response.put("id", certificate.getId());
        response.put("employeeId", certificate.getEmployeeId());
        response.put("originalFileName", certificate.getOriginalFileName());
        response.put("storedFileName", certificate.getStoredFileName());
        response.put("contentType", certificate.getContentType());
        response.put("sizeBytes", certificate.getSizeBytes());
        response.put("uploadedBy", certificate.getUploadedBy());
        response.put("uploadedAt", certificate.getUploadedAt());
        response.put("status", certificate.getStatus());

        return response;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}