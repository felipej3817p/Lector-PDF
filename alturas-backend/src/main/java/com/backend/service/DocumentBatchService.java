package com.backend.service;

import com.backend.model.EmailLog;
import com.backend.model.Employee;
import com.backend.model.HistoricalImportIssue;
import com.backend.model.ManagedDocument;
import com.backend.model.DocumentBatch;
import com.backend.repository.EmployeeRepository;
import com.backend.repository.HistoricalImportIssueRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocumentBatchService {

    private static final Map<String, Integer> SPANISH_MONTHS = Map.ofEntries(
            Map.entry("ene", 1),
            Map.entry("enero", 1),
            Map.entry("feb", 2),
            Map.entry("febrero", 2),
            Map.entry("mar", 3),
            Map.entry("marzo", 3),
            Map.entry("abr", 4),
            Map.entry("abril", 4),
            Map.entry("may", 5),
            Map.entry("mayo", 5),
            Map.entry("jun", 6),
            Map.entry("junio", 6),
            Map.entry("jul", 7),
            Map.entry("julio", 7),
            Map.entry("ago", 8),
            Map.entry("agosto", 8),
            Map.entry("sep", 9),
            Map.entry("sept", 9),
            Map.entry("septiembre", 9),
            Map.entry("oct", 10),
            Map.entry("octubre", 10),
            Map.entry("nov", 11),
            Map.entry("noviembre", 11),
            Map.entry("dic", 12),
            Map.entry("diciembre", 12));

    private final EmployeeRepository employeeRepository;
    private final DocumentService documentService;
    private final DocumentAnalysisService documentAnalysisService;
    private final PdfTextExtractorService pdfTextExtractorService;
    private final PdfFieldParserService pdfFieldParserService;
    private final DocumentBatchFacadeService batchFacadeService;
    private final EmailSendService emailSendService;
    private final HistoricalImportIssueRepository historicalImportIssueRepository;
    private final TrainingCertificateService trainingCertificateService;

    @Value("${app.storage.historical-issues-dir:uploads/historical-issues}")
    private String historicalIssuesDir;

    public DocumentBatchService(
            EmployeeRepository employeeRepository,
            DocumentService documentService,
            DocumentAnalysisService documentAnalysisService,
            PdfTextExtractorService pdfTextExtractorService,
            PdfFieldParserService pdfFieldParserService,
            DocumentBatchFacadeService batchFacadeService,
            EmailSendService emailSendService,
            HistoricalImportIssueRepository historicalImportIssueRepository,
            @Lazy TrainingCertificateService trainingCertificateService) {
        this.employeeRepository = employeeRepository;
        this.documentService = documentService;
        this.documentAnalysisService = documentAnalysisService;
        this.pdfTextExtractorService = pdfTextExtractorService;
        this.pdfFieldParserService = pdfFieldParserService;
        this.batchFacadeService = batchFacadeService;
        this.emailSendService = emailSendService;
        this.historicalImportIssueRepository = historicalImportIssueRepository;
        this.trainingCertificateService = trainingCertificateService;
    }

    public Map<String, Object> uploadAndAnalyze(
            List<MultipartFile> files,
            String documentType,
            String examType,
            String uploadedBy) {
        return uploadAndAnalyze(files, documentType, examType, uploadedBy, "REGULAR");
    }

    public Map<String, Object> uploadAndAnalyze(
            List<MultipartFile> files,
            String documentType,
            String examType,
            String uploadedBy,
            String uploadType) {
        List<Map<String, Object>> results = new ArrayList<>();
        boolean historical = "HISTORICAL".equals(uploadType);
        boolean storageOnly = false;

        DocumentBatch batch = batchFacadeService.createBatch(
                uploadedBy,
                files != null ? files.size() : 0);

        if (files == null || files.isEmpty()) {
            batchFacadeService.completeBatch(batch, results, historical);
            return buildResponse(batch, results);
        }

        for (MultipartFile file : files) {
            results.add(processOneFile(
                    file,
                    documentType,
                    examType,
                    uploadedBy,
                    batch.getId(),
                    batch.getBatchCode(),
                    uploadType,
                    storageOnly));
        }

        batchFacadeService.completeBatch(batch, results, historical);

        /*
         * Primer envio real:
         * Despues de cargar y analizar los PDFs, se notifica al aprobador.
         * No se envia correo al trabajador en este punto.
         */
        if (historical) {
            batch.setApproverNotificationStatus("NOT_PENDING");
            batch.setApproverNotificationError("Carga historica: no se enviaron correos.");
            batch.setNotes("HISTORICAL_ANALYZED");
            batch = batchFacadeService.save(batch);
        } else if (batch.getSuccessCount() > 0) {
            EmailLog approverLog = emailSendService.sendBatchSummaryEmail(batch.getId());

            batch.setApproverNotificationStatus(approverLog.getStatus());
            batch.setApproverNotificationError(approverLog.getErrorMessage());

            if ("SENT".equals(approverLog.getStatus())) {
                batch.setApproverNotifiedAt(approverLog.getSentAt());
            }
            batch = batchFacadeService.save(batch);
        }

        return buildResponse(batch, results);
    }

    private Map<String, Object> buildResponse(
            DocumentBatch batch,
            List<Map<String, Object>> results) {
        Map<String, Object> response = new LinkedHashMap<>();

        response.put("batchId", batch.getId());
        response.put("batchCode", batch.getBatchCode());
        response.put("batchStatus", batch.getStatus());
        response.put("total", batch.getTotalFiles());
        response.put("success", batch.getSuccessCount());
        response.put("failed", batch.getFailedCount());
        response.put("pendingReview", batch.getPendingReviewCount());
        response.put("apt", batch.getAptCount());
        response.put("notApt", batch.getNotAptCount());
        response.put("approverNotificationStatus", batch.getApproverNotificationStatus());
        response.put("approverNotifiedAt", batch.getApproverNotifiedAt());
        response.put("approverNotificationError", batch.getApproverNotificationError());
        response.put("historical", historicalBatch(batch));
        response.put("storageOnly", "STORAGE_ONLY".equals(safe(batch.getNotes())));
        response.put("results", results);

        return response;
    }

    private boolean historicalBatch(DocumentBatch batch) {
        String status = batch != null ? safe(batch.getStatus()) : "";
        return "HISTORICAL_COMPLETED".equals(status) || "HISTORICAL_PARTIAL_ERROR".equals(status);
    }

    private Map<String, Object> processOneFile(
            MultipartFile file,
            String documentType,
            String examType,
            String uploadedBy,
            String batchId,
            String batchCode,
            String uploadType,
            boolean storageOnly) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("fileName", file != null ? file.getOriginalFilename() : "");
        String documentNumber = "";
        String patientName = "";
        String birthDate = "";
        LocalDate fechaConcepto = null;

        boolean historical = "HISTORICAL".equals(uploadType);
        boolean isConstancia = "CONSTANCIA".equals(uploadType);

        try {
            validateFile(file);

            if (isConstancia) {
                String originalFilename = file != null ? file.getOriginalFilename() : "";
                if (originalFilename != null) {
                    documentNumber = originalFilename.replaceAll("(?i)\\.pdf$", "").replaceAll("[^0-9]", "");
                }
            } else {
                Map<String, Object> extractedFields = extractIdentityFields(file);
                documentNumber = normalizeDocumentNumber(extractedFields.getOrDefault("documentNumber", ""));
                patientName = safe(String.valueOf(extractedFields.getOrDefault("patientName", "")));
                birthDate = safe(String.valueOf(extractedFields.getOrDefault("birthDate", "")));
                fechaConcepto = resolveFechaConcepto(extractedFields);
            }

            if (documentNumber.isBlank()) {
                throw new IllegalArgumentException("No se pudo extraer la cedula del PDF"
                        + (!patientName.isBlank() ? " de " + patientName : "")
                        + ". El archivo se omitio y la carga continua.");
            }

            final String resolvedName = patientName;
            final String resolvedDoc = documentNumber;
            Employee employee = resolveEmployee(resolvedDoc, resolvedName, historical || isConstancia)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No se encontro trabajador para documento " + resolvedDoc
                                    + (!resolvedName.isBlank() ? " / nombre " + resolvedName : "")
                                    + ". El archivo se omitio y la carga continua."));

            if (isConstancia) {
                trainingCertificateService.uploadCertificate(employee.getId(), file);

                item.put("status", "OK");
                item.put("message", "Constancia cargada correctamente.");
                item.put("documentId", "");
                item.put("employeeId", employee.getId());
                item.put("employeeName", buildEmployeeName(employee));
                item.put("employeeDocument", documentNumber);
                item.put("areaCode", employee.getAreaCode() != null ? employee.getAreaCode().name() : "");
                item.put("fechaEvaluacion", null);
                item.put("evaluationDate", null);
                item.put("birthDate", "");
                item.put("fechaNacimiento", "");
                item.put("resultStatus", "");
                item.put("reviewStatus", "");
                item.put("notificationStatus", "");
                return item;
            }

            ManagedDocument saved = documentService.uploadDocument(
                    employee.getId(),
                    documentType,
                    examType,
                    file,
                    uploadedBy);

            saved.setBatchId(batchId);
            saved.setBatchCode(batchCode);
            saved.setHistorical(historical);
            documentService.save(saved);

            String resultStatus = "";

            if (storageOnly) {
                saved = documentService.getDocumentById(saved.getId());
                saved.setFechaConcepto(fechaConcepto);
                saved.setReviewStatus("");
                saved.setReviewedBy("");
                saved.setReviewedAt(null);
                saved.setReviewComment("Carga historica. PDF almacenado sin analisis ni envio de correos.");
                saved.setProcessingStatus("STORED");
                saved.setNotificationStatus("");
                saved.setNotificationError("");
                saved = documentService.save(saved);
            } else {
                Map<String, Object> analysis = documentAnalysisService.analyze(saved.getId());
                resultStatus = String.valueOf(analysis.getOrDefault("resultStatus", ""));

                if (historical) {
                    saved = documentService.getDocumentById(saved.getId());
                    saved.setReviewStatus("NOT_PENDING");
                    saved.setReviewedBy("");
                    saved.setReviewedAt(null);
                    saved.setReviewComment("Carga historica. Documento almacenado y analizado solo para historial.");
                    saved.setProcessingStatus("STORED");
                    saved.setNotificationStatus("NOT_PENDING");
                    saved.setNotificationError("Carga historica: no se enviaron correos.");
                    saved = documentService.save(saved);
                }
            }

            item.put("status", "OK");
            item.put("message", storageOnly
                    ? "PDF guardado como historial. No se analizo y no se enviaron correos."
                    : historical
                            ? "Cargado como historico. Se leyo el resultado y no se enviaron correos."
                            : "Cargado y analizado correctamente. Pendiente de revision.");
            item.put("documentId", saved.getId());
            item.put("employeeId", employee.getId());
            item.put("employeeName", buildEmployeeName(employee));
            item.put("employeeDocument", documentNumber);
            item.put("areaCode", employee.getAreaCode() != null ? employee.getAreaCode().name() : "");
            item.put("fechaEvaluacion", fechaConcepto);
            item.put("evaluationDate", fechaConcepto);
            item.put("birthDate", birthDate);
            item.put("fechaNacimiento", birthDate);
            item.put("resultStatus", storageOnly ? "" : resultStatus);
            item.put("reviewStatus", storageOnly ? "" : saved.getReviewStatus() != null ? saved.getReviewStatus() : "PENDING_REVIEW");
            item.put("notificationStatus",
                    saved.getNotificationStatus() != null ? saved.getNotificationStatus() : "NOT_PENDING");
        } catch (Exception ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : "Error procesando el archivo.";
            item.put("status", "ERROR");
            item.put("message", message);
            item.put("employeeName", patientName);
            item.put("employeeDocument", documentNumber);
            item.put("areaCode", "");
            item.put("birthDate", birthDate);
            item.put("fechaNacimiento", birthDate);
            item.put("fechaEvaluacion", fechaConcepto);
            item.put("evaluationDate", fechaConcepto);
            item.put("resultStatus", "");
            item.put("reviewStatus", "");
            item.put("notificationStatus", "");

            if (historical || isConstancia) {
                saveHistoricalIssue(batchId, batchCode, item, documentNumber, patientName, message, uploadedBy, file, uploadType);
            }
        }

        return item;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo esta vacio.");
        }

        String originalName = file.getOriginalFilename();

        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Solo se permiten archivos PDF.");
        }
    }

    private Map<String, Object> extractIdentityFields(MultipartFile file) throws Exception {
        Path tempFile = Files.createTempFile("pdf-batch-", ".pdf");

        try {
            /*
             * No usar file.transferTo(...)
             * porque puede consumir/mover el archivo temporal del request.
             * Luego DocumentService no podria guardar el PDF.
             */
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            String extractedText = pdfTextExtractorService.extractText(tempFile);
            return pdfFieldParserService.extractFields(extractedText);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    public List<HistoricalImportIssue> getHistoricalImportIssues() {
        return historicalImportIssueRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<HistoricalImportIssue> getHistoricalImportIssuesByBatch(String batchId) {
        return historicalImportIssueRepository.findByBatchIdOrderByCreatedAtDesc(batchId);
    }

    public void deleteHistoricalImportIssue(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("No se recibio el identificador del PDF no asociado.");
        }

        if (!historicalImportIssueRepository.existsById(id)) {
            throw new IllegalArgumentException("El registro de PDF no asociado no existe.");
        }

        HistoricalImportIssue issue = historicalImportIssueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El registro de PDF no asociado no existe."));

        deleteIssueFile(issue);
        historicalImportIssueRepository.delete(issue);
    }

    public void deleteAllHistoricalImportIssues() {
        historicalImportIssueRepository.findAll().forEach(this::deleteIssueFile);
        historicalImportIssueRepository.deleteAll();
    }

    public Resource getHistoricalImportIssueFile(String id) {
        HistoricalImportIssue issue = historicalImportIssueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El registro de PDF no asociado no existe."));

        if (safe(issue.getFilePath()).isBlank()) {
            throw new IllegalArgumentException("Este registro no tiene PDF guardado. Vuelve a cargar el archivo historico para conservarlo.");
        }

        Path path = Paths.get(issue.getFilePath());

        if (!Files.exists(path)) {
            throw new IllegalArgumentException("No se encontro el archivo fisico del PDF no asociado.");
        }

        return new FileSystemResource(path);
    }

    private void saveHistoricalIssue(
            String batchId,
            String batchCode,
            Map<String, Object> item,
            String documentNumber,
            String patientName,
            String message,
            String uploadedBy,
            MultipartFile file,
            String uploadType) {
        HistoricalImportIssue issue = new HistoricalImportIssue();
        issue.setBatchId(batchId);
        issue.setBatchCode(batchCode);
        issue.setFileName(String.valueOf(item.getOrDefault("fileName", "")));
        issue.setDocumentNumber(documentNumber);
        issue.setPatientName(patientName);
        Object evaluationDate = item.get("fechaEvaluacion");
        if (evaluationDate instanceof LocalDate localDate) {
            issue.setFechaEvaluacion(localDate);
        }
        storeHistoricalIssueFile(issue, file, uploadedBy);
        issue.setMessage(message);
        issue.setUploadedBy(uploadedBy);
        issue.setCreatedAt(LocalDateTime.now());
        issue.setStatus("PENDING");
        issue.setUploadType(uploadType);
        historicalImportIssueRepository.save(issue);
    }

    private void storeHistoricalIssueFile(HistoricalImportIssue issue, MultipartFile file, String uploadedBy) {
        if (file == null || file.isEmpty()) {
            return;
        }

        String originalName = file.getOriginalFilename();

        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
            return;
        }

        try {
            Path uploadPath = Paths.get(historicalIssuesDir)
                    .resolve(sanitizePathSegment(uploadedBy));

            Files.createDirectories(uploadPath);

            String safeOriginalName = sanitizeFileName(originalName);
            String storedFileName = UUID.randomUUID() + "_" + safeOriginalName;
            Path targetPath = uploadPath.resolve(storedFileName);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            issue.setStoredFileName(storedFileName);
            issue.setFilePath(targetPath.toString());
            issue.setContentType(file.getContentType());
        } catch (IOException ignored) {
            // La incidencia se guarda aunque no se pueda conservar la copia fisica.
        }
    }

    private void deleteIssueFile(HistoricalImportIssue issue) {
        if (issue == null || safe(issue.getFilePath()).isBlank()) {
            return;
        }

        try {
            Files.deleteIfExists(Paths.get(issue.getFilePath()));
        } catch (IOException ignored) {
            // No bloquea la limpieza del registro.
        }
    }

    private String sanitizeFileName(String fileName) {
        return String.valueOf(fileName == null ? "archivo.pdf" : fileName)
                .replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String sanitizePathSegment(String value) {
        String sanitized = String.valueOf(value == null ? "" : value)
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+", "")
                .replaceAll("_+$", "");

        return sanitized.isBlank() ? "sin_valor" : sanitized;
    }

    private LocalDate resolveFechaConcepto(Map<String, Object> extractedFields) {
        if (extractedFields == null || extractedFields.isEmpty()) {
            return null;
        }

        String[] keys = {
                "fechaEvaluacion",
                "evaluationDate",
                "fechaConcepto",
                "conceptDate",
                "examDate",
                "fechaExamen",
                "date",
                "fecha"
        };

        for (String key : keys) {
            LocalDate parsed = parseDate(String.valueOf(extractedFields.getOrDefault(key, "")));

            if (parsed != null) {
                return parsed;
            }
        }

        return null;
    }

    private LocalDate parseDate(String value) {
        String raw = safe(value);

        if (raw.isBlank()) {
            return null;
        }

        LocalDate parsedSpanishDate = parseSpanishTextDate(raw);
        if (parsedSpanishDate != null) {
            return parsedSpanishDate;
        }

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("d-M-yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(raw, formatter);
            } catch (DateTimeParseException ignored) {
                // Intenta con el siguiente formato.
            }
        }

        return null;
    }

    private LocalDate parseSpanishTextDate(String value) {
        String raw = Normalizer.normalize(safe(value), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replace(",", " ");

        Matcher matcher = Pattern
                .compile("\\b(\\d{1,2})\\s+(?:de\\s+)?([a-z]+)\\.?\\s+(?:de\\s+)?(\\d{4})\\b")
                .matcher(raw);

        if (!matcher.find()) {
            return null;
        }

        Integer month = SPANISH_MONTHS.get(matcher.group(2));
        if (month == null) {
            return null;
        }

        try {
            return LocalDate.of(
                    Integer.parseInt(matcher.group(3)),
                    month,
                    Integer.parseInt(matcher.group(1)));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private Optional<Employee> resolveEmployee(String documentNumber, String patientName, boolean historical) {
        Optional<Employee> byDocument = employeeRepository.findByDocumentNumber(documentNumber);

        if (byDocument.isPresent()) {
            return byDocument;
        }

        Optional<Employee> byNormalizedDocument = employeeRepository.findAll().stream()
                .filter(employee -> normalizeDocumentNumber(employee.getDocumentNumber()).equals(documentNumber))
                .findFirst();

        if (byNormalizedDocument.isPresent() || !historical || patientName.isBlank()) {
            return byNormalizedDocument;
        }

        String normalizedPatientName = normalizeName(patientName);

        Employee bestMatch = null;
        int bestScore = 0;
        boolean duplicatedBestScore = false;

        for (Employee employee : employeeRepository.findAll()) {
            int score = nameSimilarityScore(normalizedPatientName, normalizeName(buildEmployeeName(employee)));

            if (score < 2) {
                continue;
            }

            if (score > bestScore) {
                bestMatch = employee;
                bestScore = score;
                duplicatedBestScore = false;
            } else if (score == bestScore) {
                duplicatedBestScore = true;
            }
        }

        if (bestMatch != null && !duplicatedBestScore) {
            return Optional.of(bestMatch);
        }

        return Optional.empty();
    }

    private int nameSimilarityScore(String left, String right) {
        if (left.isBlank() || right.isBlank()) {
            return 0;
        }

        if (left.equals(right)) {
            return 10;
        }

        List<String> leftTokens = List.of(left.split("\\s+"));
        List<String> rightTokens = List.of(right.split("\\s+"));

        int score = 0;

        for (String token : leftTokens) {
            if (token.length() >= 3 && rightTokens.contains(token)) {
                score++;
            }
        }

        return score;
    }

    private String normalizeName(String value) {
        String normalized = Normalizer.normalize(safe(value), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase()
                .replaceAll("[^A-Z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        return normalized;
    }

    private String normalizeDocumentNumber(Object value) {
        return String.valueOf(value == null ? "" : value)
                .replaceAll("\\D", "")
                .trim();
    }

    private String buildEmployeeName(Employee employee) {
        return String.join(
                " ",
                safe(employee.getFirstName()),
                safe(employee.getSecondName()),
                safe(employee.getFirstLastName()),
                safe(employee.getSecondLastName()))
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
