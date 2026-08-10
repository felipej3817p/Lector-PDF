package com.backend.service;

import com.backend.model.AreaCode;
import com.backend.model.DocumentAnalysis;
import com.backend.model.Employee;
import com.backend.model.ManagedDocument;
import com.backend.model.User;
import com.backend.repository.DocumentAnalysisRepository;
import com.backend.repository.EmployeeRepository;
import com.backend.repository.ManagedDocumentRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private static final Charset CSV_CHARSET = Charset.forName("ISO-8859-1");
    private static final DateTimeFormatter CSV_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private final EmployeeRepository employeeRepository;
    private final ManagedDocumentRepository managedDocumentRepository;
    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final AccessScopeService accessScopeService;

    @Value("${app.reports.ministry.default-country:COLOMBIA}")
    private String defaultCountry;

    @Value("${app.reports.ministry.default-education-level:}")
    private String defaultProfession;

    @Value("${app.reports.ministry.default-sector:Sector minero y energetico}")
    private String defaultEconomicSector;

    @Value("${app.reports.ministry.default-company:Empresa de energia de Boyaca}")
    private String defaultCompany;

    @Value("${app.reports.ministry.default-arl:Axa colpatria}")
    private String defaultArl;

    @Value("${app.reports.ministry.default-process:Distribucion}")
    private String defaultProcess;

    public ReportService(
            EmployeeRepository employeeRepository,
            ManagedDocumentRepository managedDocumentRepository,
            DocumentAnalysisRepository documentAnalysisRepository,
            AccessScopeService accessScopeService) {
        this.employeeRepository = employeeRepository;
        this.managedDocumentRepository = managedDocumentRepository;
        this.documentAnalysisRepository = documentAnalysisRepository;
        this.accessScopeService = accessScopeService;
    }

    /*
     * Excel:
     * mode=latest -> último concepto por trabajador.
     * mode=history -> histórico completo.
     */
    public byte[] generateAptitudeExcel(Map<String, String> filters) {
        List<AptitudeReportRow> rows = buildAptitudeRows(filters);

        if (rows.isEmpty()) {
            throw new com.backend.exception.AppException(
                    com.backend.exception.ErrorCode.VALIDATION_ERROR,
                    "No hay ningún trabajador evaluado.");
        }

        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("REPORTE");

            CellStyle titleStyle = buildTitleStyle(workbook);
            CellStyle headerStyle = buildHeaderStyle(workbook);
            CellStyle bodyStyle = buildBodyStyle(workbook);
            CellStyle dateStyle = buildDateStyle(workbook);
            CellStyle greenStyle = buildStatusStyle(
                    workbook,
                    IndexedColors.LIGHT_GREEN.getIndex(),
                    IndexedColors.DARK_GREEN.getIndex());
            CellStyle redStyle = buildStatusStyle(
                    workbook,
                    IndexedColors.CORAL.getIndex(),
                    IndexedColors.DARK_RED.getIndex());
            CellStyle yellowStyle = buildStatusStyle(
                    workbook,
                    IndexedColors.LIGHT_YELLOW.getIndex(),
                    IndexedColors.DARK_YELLOW.getIndex());

            String mode = safe(filters.get("mode")).equalsIgnoreCase("history")
                    ? "HISTÓRICO COMPLETO"
                    : "ÚLTIMO CONCEPTO POR TRABAJADOR";

            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(28);

            createMergedCell(
                    sheet,
                    0,
                    0,
                    0,
                    5,
                    "REPORTE CONCEPTOS DE APTITUD ALTURAS",
                    titleStyle);

            Row modeRow = sheet.createRow(1);
            modeRow.setHeightInPoints(22);

            createMergedCell(
                    sheet,
                    1,
                    0,
                    1,
                    5,
                    "TIPO DE REPORTE: " + mode,
                    bodyStyle);

            Row headerRow = sheet.createRow(3);
            headerRow.setHeightInPoints(42);

            String[] headers = {
                    "Nombre del trabajador",
                    "Cédula de ciudadanía",
                    "Cargo",
                    "Zona/ área",
                    "Resultado concepto de aptitud para trabajo en alturas",
                    "Fecha de concepto médico"
            };

            for (int i = 0; i < headers.length; i++) {
                createTextCell(headerRow, i, headers[i], headerStyle);
            }

            int rowIndex = 4;

            for (AptitudeReportRow reportRow : rows) {
                Row row = sheet.createRow(rowIndex++);
                row.setHeightInPoints(30);

                createTextCell(row, 0, reportRow.workerName(), bodyStyle);
                createTextCell(row, 1, reportRow.documentNumber(), bodyStyle);
                createTextCell(row, 2, reportRow.position(), bodyStyle);
                createTextCell(row, 3, reportRow.area(), bodyStyle);

                String resultLabel = reportRow.resultLabel();
                String normalized = normalizeResultStatus(resultLabel);

                CellStyle cellStyle = bodyStyle;

                if ("APTO".equals(normalized)) {
                    cellStyle = greenStyle;
                } else if ("NO_APTO".equals(normalized)) {
                    cellStyle = redStyle;
                } else if ("PENDIENTE".equals(normalized)) {
                    cellStyle = yellowStyle;
                }

                createTextCell(row, 4, resultLabel, cellStyle);

                Cell dateCell = row.createCell(5);

                if (reportRow.conceptDate() != null) {
                    dateCell.setCellValue(java.sql.Date.valueOf(reportRow.conceptDate()));
                    dateCell.setCellStyle(dateStyle);
                } else {
                    dateCell.setCellValue("");
                    dateCell.setCellStyle(bodyStyle);
                }

            }

            sheet.createFreezePane(0, 4);

            sheet.setColumnWidth(0, 34 * 256);
            sheet.setColumnWidth(1, 22 * 256);
            sheet.setColumnWidth(2, 34 * 256);
            sheet.setColumnWidth(3, 28 * 256);
            sheet.setColumnWidth(4, 52 * 256);
            sheet.setColumnWidth(5, 24 * 256);

            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el reporte Excel.", ex);
        }
    }

    /*
     * CSV trabajadores / archivo plano.
     *
     * Orden exacto:
     * Tipo documento;Número documento;Primer nombre;Segundo nombre;Primer
     * apellido;Segundo apellido;Género;País;Fecha
     * nacimiento;Profesión;Área;Cargo;Sector;Empresa;ARL
     *
     * Sin encabezado.
     * Separador: ;
     */
    public byte[] generateMinistryCsv(Map<String, String> filters) {
        String filterValue = filters.get("resultStatus");
        String resultStatusFilter = normalizeResultStatus(filterValue);

        LocalDate uploadedFrom = parseDate(firstNonBlank(filters.get("uploadedFrom"), filters.get("from")));
        LocalDate uploadedTo = parseDate(firstNonBlank(filters.get("uploadedTo"), filters.get("to")));

        List<MinistryCsvRow> rows = getAccessibleEmployees().stream()
                .map(employee -> {
                    List<ManagedDocument> docs = managedDocumentRepository
                            .findByEmployeeIdOrderByUploadedAtDesc(employee.getId())
                            .stream()
                            .filter(doc -> {
                                String rs = safe(doc.getReviewStatus());
                                return "APPROVED".equalsIgnoreCase(rs) && !doc.isHistorical();
                            })
                            .toList();
                    if (docs.isEmpty())
                        return null;
                    ManagedDocument latestDocument = docs.get(0);
                    MinistryCsvRow row = toMinistryCsvRowOrNull(latestDocument, filters, resultStatusFilter);
                    if (row == null)
                        return null;
                    LocalDate conceptDate = row.conceptDateSafe();
                    if (uploadedFrom != null && (conceptDate == null || conceptDate.isBefore(uploadedFrom)))
                        return null;
                    if (uploadedTo != null && (conceptDate == null || conceptDate.isAfter(uploadedTo)))
                        return null;
                    return row;
                })
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(MinistryCsvRow::uploadedAtSafe, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(row -> fullName(row.employee()), String.CASE_INSENSITIVE_ORDER))
                .toList();

        if (rows.isEmpty()) {
            throw new com.backend.exception.AppException(
                    com.backend.exception.ErrorCode.VALIDATION_ERROR,
                    "No hay ningún trabajador evaluado.");
        }

        StringBuilder csv = new StringBuilder();

        for (MinistryCsvRow reportRow : rows) {
            Employee employee = reportRow.employee();

            List<String> columns = List.of(
                    defaultIfBlank(employee.getDocumentType(), "CC"),
                    safe(employee.getDocumentNumber()),
                    safe(employee.getFirstName()),
                    safe(employee.getSecondName()),
                    safe(employee.getFirstLastName()),
                    safe(employee.getSecondLastName()),
                    resolveGender(employee.getGender()),
                    defaultIfBlank(defaultCountry, "COLOMBIA"),
                    formatCsvDate(employee.getBirthDate()),
                    safe(employee.getEducationalLevel()),
                    defaultIfBlank(defaultProcess, "Distribucion"), // Distribución por defecto
                    safe(employee.getCurrentPosition()),
                    defaultIfBlank(defaultEconomicSector, "Sector minero y energetico"),
                    defaultIfBlank(defaultCompany, "Empresa de energia de Boyaca"),
                    defaultIfBlank(defaultArl, "Axa colpatria seguros de vida"));

            csv.append(columns.stream()
                    .map(this::escapeCsv)
                    .collect(Collectors.joining(";")));

            csv.append("\r\n");
        }

        return csv.toString().getBytes(CSV_CHARSET);
    }

    /*
     * Busca la fechaConcepto más reciente del trabajador.
     * Si resultStatusFilter viene con APTO / NO_APTO, solo toma documentos con ese
     * resultado.
     */
    private MinistryCsvRow toMinistryCsvRowOrNull(
            ManagedDocument document,
            Map<String, String> filters,
            String resultStatusFilter) {
        if (safe(document.getEmployeeId()).isBlank()) {
            return null;
        }

        Optional<DocumentAnalysis> analysisOptional = documentAnalysisRepository.findByDocumentId(document.getId());

        if (analysisOptional.isEmpty()) {
            return null;
        }

        DocumentAnalysis analysis = analysisOptional.get();
        String documentResultStatus = normalizeResultStatus(analysis.getResultStatus());
        LocalDate conceptDate = resolveConceptDate(document, analysis);

        if (isExpired(conceptDate)) {
            documentResultStatus = "VIGENCIA_VENCIDA";
        }

        if (!safe(resultStatusFilter).isBlank()) {
            if (!documentResultStatus.equals(resultStatusFilter)) {
                return null;
            }
        } else if (!"APTO".equals(documentResultStatus) && !"NO_APTO".equals(documentResultStatus)
                && !"VIGENCIA_VENCIDA".equals(documentResultStatus)) {
            return null;
        }

        Employee employee = employeeRepository.findById(document.getEmployeeId()).orElse(null);

        if (employee == null || !matchesEmployeeFilters(employee, filters)) {
            return null;
        }

        return new MinistryCsvRow(employee, document, analysis, conceptDate);
    }

    private List<AptitudeReportRow> buildAptitudeRows(Map<String, String> filters) {
        String mode = safe(filters.get("mode")).toLowerCase(Locale.ROOT);
        boolean historyMode = "history".equals(mode);

        String filterValue = filters.get("resultStatus");
        String resultStatusFilter = normalizeResultStatus(filterValue);

        LocalDate from = parseDate(filters.get("from"));
        LocalDate to = parseDate(filters.get("to"));

        List<Employee> employees = getAccessibleEmployees().stream()
                .filter(employee -> matchesEmployeeFilters(employee, filters))
                .toList();

        List<AptitudeReportRow> rows = new ArrayList<>();

        for (Employee employee : employees) {
            List<ManagedDocument> documents = managedDocumentRepository
                    .findByEmployeeIdOrderByUploadedAtDesc(employee.getId())
                    .stream()
                    .filter(doc -> {
                        if (historyMode) {
                            return doc.isHistorical();
                        } else {
                            String rs = safe(doc.getReviewStatus());
                            return "APPROVED".equalsIgnoreCase(rs) && !doc.isHistorical();
                        }
                    })
                    .toList();

            if (documents.isEmpty()) {
                continue;
            }

            if (!historyMode) {
                documents = List.of(documents.get(0));
            }

            List<AptitudeReportRow> employeeRows = new ArrayList<>();

            for (ManagedDocument document : documents) {
                Optional<DocumentAnalysis> analysisOptional = documentAnalysisRepository
                        .findByDocumentId(document.getId());

                if (analysisOptional.isEmpty()) {
                    continue;
                }

                DocumentAnalysis analysis = analysisOptional.get();
                String resultStatus = normalizeResultStatus(analysis.getResultStatus());
                LocalDate conceptDate = resolveConceptDate(document, analysis);

                if (!historyMode && isExpired(conceptDate)) {
                    resultStatus = "VIGENCIA_VENCIDA";
                }

                if (!resultStatusFilter.isBlank()) {
                    if (!resultStatus.equals(resultStatusFilter)) {
                        continue;
                    }
                } else if (!"APTO".equals(resultStatus) && !"NO_APTO".equals(resultStatus)
                        && !"VIGENCIA_VENCIDA".equals(resultStatus)) {
                    continue;
                }

                if ((from != null || to != null) && conceptDate == null) {
                    continue;
                }

                if (from != null && conceptDate.isBefore(from)) {
                    continue;
                }

                if (to != null && conceptDate.isAfter(to)) {
                    continue;
                }

                String cargo = safe(employee.getCurrentPosition());
                if (cargo.isBlank() && analysis.getExtractedFields() != null) {
                    cargo = firstNonBlank(
                            String.valueOf(analysis.getExtractedFields().getOrDefault("position", "")),
                            String.valueOf(analysis.getExtractedFields().getOrDefault("cargo", "")));
                }

                employeeRows.add(new AptitudeReportRow(
                        fullName(employee),
                        safe(employee.getDocumentNumber()),
                        cargo,
                        resolveAreaLabel(employee),
                        resultLabel(resultStatus),
                        conceptDate));
            }

            employeeRows.sort(
                    Comparator.comparing(
                            AptitudeReportRow::conceptDate,
                            Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(AptitudeReportRow::workerName, String.CASE_INSENSITIVE_ORDER));

            if (historyMode) {
                rows.addAll(employeeRows);
            } else if (!employeeRows.isEmpty()) {
                rows.add(employeeRows.get(0));
            }
        }

        rows.sort(
                Comparator.comparing(
                        AptitudeReportRow::conceptDate,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AptitudeReportRow::workerName, String.CASE_INSENSITIVE_ORDER));

        return rows;
    }

    private List<Employee> getAccessibleEmployees() {
        User currentUser = accessScopeService.getCurrentUser();

        if (accessScopeService.hasGlobalDocumentAccess(currentUser)) {
            return employeeRepository.findAll();
        }

        Set<AreaCode> allowedAreas = accessScopeService.getAllowedAreas(currentUser);

        if (allowedAreas == null || allowedAreas.isEmpty()) {
            return List.of();
        }

        return employeeRepository.findByAreaCodeIn(allowedAreas);
    }

    private boolean matchesEmployeeFilters(Employee employee, Map<String, String> filters) {
        String documentNumber = safe(filters.get("documentNumber"));

        if (!documentNumber.isBlank() && !safe(employee.getDocumentNumber()).contains(documentNumber)) {
            return false;
        }

        String areaFilter = safe(filters.get("areaCode"));

        if (!areaFilter.isBlank()) {
            boolean matches = (employee.getAreaCode() != null
                    && employee.getAreaCode().name().equalsIgnoreCase(areaFilter))
                    || safe(employee.getZone()).equalsIgnoreCase(areaFilter)
                    || safe(employee.getWorkArea()).equalsIgnoreCase(areaFilter);

            if (!matches) {
                return false;
            }
        }

        String name = normalizeSearchText(filters.get("name"));

        if (!name.isBlank() && !normalizeSearchText(fullName(employee)).contains(name)) {
            return false;
        }

        String position = normalizeSearchText(filters.get("position"));

        if (!position.isBlank() && !normalizeSearchText(employee.getCurrentPosition()).contains(position)) {
            return false;
        }

        String enabled = safe(filters.get("enabled"));
        if (!enabled.isBlank()) {
            boolean isEnabled = Boolean.parseBoolean(enabled);
            if (employee.isActive() != isEnabled) {
                return false;
            }
        }

        return true;
    }

    private LocalDate resolveConceptDate(ManagedDocument document, DocumentAnalysis analysis) {
        if (document != null && document.getFechaConcepto() != null) {
            return document.getFechaConcepto();
        }

        if (analysis == null) {
            return null;
        }

        if (analysis.getEvaluationDate() != null) {
            return analysis.getEvaluationDate();
        }

        LocalDate fromFields = extractDateFromFields(analysis.getExtractedFields());

        if (fromFields != null) {
            return fromFields;
        }

        return null;
    }

    private LocalDate extractDateFromFields(Map<String, Object> fields) {
        if (fields == null || fields.isEmpty()) {
            return null;
        }

        String[] possibleKeys = {
                "evaluationDate",
                "fechaEvaluacion",
                "conceptDate",
                "fechaConcepto",
                "examDate",
                "fechaExamen",
                "date",
                "fecha"
        };

        for (String key : possibleKeys) {
            Object value = fields.get(key);

            if (value != null) {
                LocalDate parsed = parseDate(String.valueOf(value));

                if (parsed != null) {
                    return parsed;
                }
            }
        }

        return null;
    }

    private LocalDate parseDate(String value) {
        String raw = safe(value);

        if (raw.isBlank()) {
            return null;
        }

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("M/d/yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("d-M-yyyy"),
                DateTimeFormatter.ofPattern("M-d-yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("MM-dd-yyyy"));

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(raw, formatter);
            } catch (DateTimeParseException ignored) {
                // Intenta con el siguiente formato.
            }
        }

        return null;
    }

    private String formatCsvDate(String value) {
        LocalDate date = parseDate(value);

        if (date == null) {
            return safe(value);
        }

        return date.format(CSV_DATE_FORMATTER);
    }

    private String normalizeResultStatus(String value) {
        String normalized = safe(value)
                .toUpperCase(Locale.ROOT)
                .replace(" ", "_")
                .trim();

        if (normalized.isBlank()) {
            return "";
        }

        if ("NO_APTO".equals(normalized) || "NO_APTO_TEMPORAL".equals(normalized)) {
            return "NO_APTO";
        }
        if ("VIGENCIA_VENCIDA".equals(normalized)) {
            return "VIGENCIA_VENCIDA";
        }
        if ("APTO".equals(normalized)) {
            return "APTO";
        }

        return normalized;
    }

    private boolean isExpired(LocalDate conceptDate) {
        if (conceptDate == null) {
            return false;
        }
        return LocalDate.now().isAfter(conceptDate.plusDays(365));
    }

    private String resultLabel(String resultStatus) {
        return switch (normalizeResultStatus(resultStatus)) {
            case "APTO" -> "Apto para trabajo en alturas";
            case "NO_APTO" -> "No apto temporalmente para trabajo en alturas";
            case "VIGENCIA_VENCIDA" -> "Vigencia vencida (requiere nueva evaluación)";
            default -> "Pendiente de validación";
        };
    }

    private String resolveGender(String gender) {
        String safeGender = safe(gender).toUpperCase(Locale.ROOT);
        if (safeGender.startsWith("F")) {
            return "F";
        }
        if (safeGender.startsWith("M")) {
            return "M";
        }
        return safeGender;
    }

    private String resolveAreaLabel(Employee employee) {
        if (!safe(employee.getZone()).isBlank()) {
            return employee.getZone();
        }

        if (!safe(employee.getWorkArea()).isBlank()) {
            return employee.getWorkArea();
        }

        if (employee.getAreaCode() != null) {
            return employee.getAreaCode().getDisplayName();
        }

        return "";
    }

    private String normalizeSearchText(String value) {
        return safe(value)
                .toUpperCase(Locale.ROOT)
                .replaceAll("[ÁÀÂÄ]", "A")
                .replaceAll("[ÉÈÊË]", "E")
                .replaceAll("[ÍÌÎÏ]", "I")
                .replaceAll("[ÓÒÔÖ]", "O")
                .replaceAll("[ÚÙÛÜ]", "U")
                .replace("Ñ", "N")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String fullName(Employee employee) {
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

    private String defaultIfBlank(String value, String defaultValue) {
        String safeValue = safe(value);
        return safeValue.isBlank() ? safe(defaultValue) : safeValue;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String safeValue = safe(value);

            if (!safeValue.isBlank()) {
                return safeValue;
            }
        }

        return "";
    }

    private String escapeCsv(String value) {
        String safeValue = safe(value);

        boolean mustQuote = safeValue.contains(";")
                || safeValue.contains("\"")
                || safeValue.contains("\n")
                || safeValue.contains("\r");

        if (!mustQuote) {
            return safeValue;
        }

        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

    private void createTextCell(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(safe(value));
        cell.setCellStyle(style);
    }

    private void createMergedCell(
            Sheet sheet,
            int firstRow,
            int firstColumn,
            int lastRow,
            int lastColumn,
            String value,
            CellStyle style) {

        Row row = sheet.getRow(firstRow);

        if (row == null) {
            row = sheet.createRow(firstRow);
        }

        Cell cell = row.createCell(firstColumn);
        cell.setCellValue(value);
        cell.setCellStyle(style);

        CellRangeAddress range = new CellRangeAddress(firstRow, lastRow, firstColumn, lastColumn);
        sheet.addMergedRegion(range);

        for (int r = firstRow; r <= lastRow; r++) {
            Row currentRow = sheet.getRow(r);

            if (currentRow == null) {
                currentRow = sheet.createRow(r);
            }

            for (int c = firstColumn; c <= lastColumn; c++) {
                Cell currentCell = currentRow.getCell(c);

                if (currentCell == null) {
                    currentCell = currentRow.createCell(c);
                }

                currentCell.setCellStyle(style);
            }
        }
    }

    private CellStyle buildTitleStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 13);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        setThinBorders(style);

        return style;
    }

    private CellStyle buildHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setFillForegroundColor(IndexedColors.GOLD.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setThinBorders(style);

        return style;
    }

    private CellStyle buildBodyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setWrapText(true);
        setThinBorders(style);

        return style;
    }

    private CellStyle buildStatusStyle(Workbook workbook, short bgColor, short textColor) {
        CellStyle style = buildBodyStyle(workbook);
        style.setFillForegroundColor(bgColor);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font font = workbook.createFont();
        font.setColor(textColor);
        font.setBold(true);
        style.setFont(font);

        return style;
    }

    private CellStyle buildDateStyle(Workbook workbook) {
        CellStyle style = buildBodyStyle(workbook);
        CreationHelper helper = workbook.getCreationHelper();
        style.setDataFormat(helper.createDataFormat().getFormat("dd/mm/yyyy"));

        return style;
    }

    private void setThinBorders(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
    }

    private record AptitudeReportRow(
            String workerName,
            String documentNumber,
            String position,
            String area,
            String resultLabel,
            LocalDate conceptDate) {
    }

    private record MinistryCsvRow(
            Employee employee,
            ManagedDocument document,
            DocumentAnalysis analysis,
            LocalDate conceptDateSafe) {

        private LocalDateTime uploadedAtSafe() {
            return document != null ? document.getUploadedAt() : null;
        }
    }
}
