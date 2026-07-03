package com.backend.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfFieldParserService {

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"));
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

    public Map<String, Object> extractFields(String text) {
        Map<String, Object> fields = new LinkedHashMap<>();

        String raw = text != null ? text.replace("\r", "") : "";
        String normalized = normalizeText(raw);
        String ascii = removeAccents(normalized);

        String patientName = firstNonBlank(
                extractNormalized(normalized, "Paciente:\\s*(.*?)\\s*Teléfono", 1),
                extractNormalized(normalized, "Nombre del trabajador:\\s*(.*?)\\s*(?:Cargo:|Área:|Concepto|Resultado)",
                        1),
                extractNormalized(normalized, "Trabajador:\\s*(.*?)\\s*(?:Cargo:|Área:|Concepto|Resultado)", 1),
                extractPatientNameFromSignature(raw));

        String documentNumber = extractDocumentNumber(raw);

        String position = firstNonBlank(
                extractNormalized(normalized, "Cargo:\\s*(.*?)\\s*Paciente:", 1),
                extractNormalized(normalized, "Cargo:\\s*(.*?)\\s*(?:Área:|Area:|Concepto|Resultado)", 1));

        String examType = extractNormalized(
                normalized,
                "Tipo de Examen:\\s*(.*?)\\s*(?:Fecha\\s*y\\s*Lugar:|Estado Civil:|Fecha Nacimiento:|Fecha de Nacimiento:|Paciente:)",
                1);

        String birthDate = extractBirthDate(normalized, ascii);

        String arl = cleanSingleLine(extractNormalized(normalized, "ARL:\\s*(.*?)\\s*AFP:", 1));

        String laborConcept = firstNonBlank(
                extractNormalized(normalized, "CONCEPTO LABORAL\\s*(.*?)\\s*Observaciones:", 1),
                extractNormalized(normalized, "Concepto para trabajo en alturas:\\s*(.*?)\\s*Resultado:", 1),
                extractNormalized(normalized, "Resultado:\\s*(APTO|NO\\s*APTO|NO_APTO)", 1));

        String observations = extractNormalized(
                normalized,
                "Observaciones:\\s*(.*?)\\s*Tipo de Restricción",
                1);

        String surveillanceProgram = extractNormalized(
                normalized,
                "Ingresar al Programa de Vigilancia Epidemiológica o Programa de Prevención y Promoción\\s*(.*?)\\s*Información de Remisiones",
                1);

        String fechaConcepto = extractConceptDate(raw, normalized);
        List<String> referrals = extractRemissions(raw);

        fields.put("patientName", cleanSingleLine(patientName));
        fields.put("documentNumber", cleanSingleLine(documentNumber));
        fields.put("position", cleanSingleLine(position));
        fields.put("examType", cleanSingleLine(examType));
        fields.put("birthDate", cleanSingleLine(birthDate));
        fields.put("arl", cleanSingleLine(arl));
        fields.put("laborConcept", cleanSingleLine(laborConcept));
        fields.put("observations", cleanSingleLine(observations));
        fields.put("surveillanceProgram", cleanSingleLine(surveillanceProgram));
        fields.put("fechaConcepto", fechaConcepto);
        fields.put("conceptDate", fechaConcepto);
        fields.put("fechaEvaluacion", fechaConcepto);
        fields.put("evaluationDate", fechaConcepto);
        fields.put("referrals", referrals);

        return fields;
    }

    private String extractBirthDate(String normalizedText, String asciiText) {
        String candidate = firstNonBlank(
                extractNormalized(normalizedText,
                        "Fecha\\s*(?:de\\s*)?Nacimiento\\s*[:\\-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})",
                        1),
                extractNormalized(asciiText,
                        "Fecha\\s*(?:de\\s*)?Nacimiento\\s*[:\\-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})",
                        1),
                extractNormalized(asciiText,
                        "Fecha\\s*(?:de\\s*)?Nacimiento\\s*[:\\-]?\\s*(\\d{1,2}\\s+(?:de\\s+)?[a-z]+\\.?\\s+(?:de\\s+)?\\d{4})",
                        1),
                extractNormalized(asciiText,
                        "Nacimiento\\s*[:\\-]?\\s*(\\d{1,2}\\s+(?:de\\s+)?[a-z]+\\.?\\s+(?:de\\s+)?\\d{4})",
                        1));

        LocalDate parsed = parseDate(candidate);

        return parsed != null ? parsed.toString() : cleanSingleLine(candidate);
    }

    private String extractConceptDate(String rawText, String normalizedText) {
        String raw = rawText == null ? "" : rawText;
        String normalized = normalizedText == null ? "" : normalizedText;
        String ascii = removeAccents(normalized);

        String fromSpecificLabels = firstNonBlank(
                extractNormalized(ascii,
                        "Fecha\\s*y\\s*Lugar\\s*[:\\-]?\\s*(\\d{1,2}\\s+(?:de\\s+)?[a-z]+\\.?\\s+(?:de\\s+)?\\d{4})",
                        1),
                extractNormalized(ascii,
                        "Fecha\\s*(?:del\\s*)?(?:concepto|examen|valoracion|evaluacion|atencion)\\s*[:\\-]?\\s*(\\d{1,2}\\s+(?:de\\s+)?[a-z]+\\.?\\s+(?:de\\s+)?\\d{4})",
                        1),
                extractNormalized(normalized,
                        "Fecha\\s*(?:del\\s*)?(?:concepto|examen|valoración|valoracion|evaluación|evaluacion|atención|atencion)\\s*[:\\-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})",
                        1),
                extractNormalized(normalized,
                        "(?:concepto|examen|valoración|valoracion|evaluación|evaluacion|atención|atencion).*?Fecha\\s*[:\\-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})",
                        1),
                extractNormalized(normalized,
                        "Fecha\\s*de\\s*Expedici[oó]n\\s*[:\\-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})", 1),
                extractNormalized(normalized,
                        "Fecha\\s*de\\s*emisi[oó]n\\s*[:\\-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})", 1),
                extractNormalized(normalized, "Fecha\\s*certificado\\s*[:\\-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})", 1),
                extractNormalized(ascii,
                        "Fecha\\s*(?:del\\s*)?(?:concepto|examen|valoracion|evaluacion|atencion)\\s*[:\\-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})",
                        1),
                extractNormalized(ascii, "Fecha\\s*de\\s*Expedicion\\s*[:\\-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})", 1),
                extractNormalized(ascii, "Fecha\\s*de\\s*emision\\s*[:\\-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})", 1));

        LocalDate parsedSpecific = parseDate(fromSpecificLabels);
        if (parsedSpecific != null) {
            return parsedSpecific.toString();
        }

        /*
         * Respaldo controlado:
         * Si el PDF no trae una etiqueta clara, se toma la última fecha válida del
         * documento,
         * evitando la fecha de nacimiento porque suele aparecer primero y no representa
         * el concepto.
         */
        List<LocalDate> candidates = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\b(\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})\\b");
        Matcher matcher = pattern.matcher(raw);

        while (matcher.find()) {
            LocalDate parsed = parseDate(matcher.group(1));
            if (parsed != null && parsed.getYear() >= 2000) {
                candidates.add(parsed);
            }
        }

        Pattern textDatePattern = Pattern.compile(
                "\\b(\\d{1,2}\\s+(?:de\\s+)?[a-z]+\\.?\\s+(?:de\\s+)?\\d{4})\\b",
                Pattern.CASE_INSENSITIVE);
        Matcher textDateMatcher = textDatePattern.matcher(ascii);

        while (textDateMatcher.find()) {
            LocalDate parsed = parseDate(textDateMatcher.group(1));
            if (parsed != null && parsed.getYear() >= 2000) {
                candidates.add(parsed);
            }
        }

        if (!candidates.isEmpty()) {
            return candidates.get(candidates.size() - 1).toString();
        }

        return "";
    }

    private LocalDate parseDate(String value) {
        String raw = cleanSingleLine(value);

        if (raw.isBlank()) {
            return null;
        }

        LocalDate parsedSpanishDate = parseSpanishTextDate(raw);
        if (parsedSpanishDate != null) {
            return parsedSpanishDate;
        }

        raw = raw.replace('.', '/');

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(raw, formatter);
            } catch (DateTimeParseException ignored) {
                // Intenta con el siguiente formato.
            }
        }

        return null;
    }

    private LocalDate parseSpanishTextDate(String value) {
        String raw = removeAccents(cleanSingleLine(value))
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

    private String extractDocumentNumber(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        String normalized = normalizeText(rawText);
        String ascii = removeAccents(normalized);

        String fromKnownLabels = firstNonBlank(
                extractNormalized(normalized, "Identificación:\\s*([\\d\\.\\s-]{5,20})", 1),
                extractNormalized(normalized, "Identificacion:\\s*([\\d\\.\\s-]{5,20})", 1),
                extractNormalized(normalized, "Número de documento:\\s*([\\d\\.\\s-]{5,20})", 1),
                extractNormalized(normalized, "Numero de documento:\\s*([\\d\\.\\s-]{5,20})", 1),
                extractNormalized(normalized, "Documento:\\s*([\\d\\.\\s-]{5,20})", 1),
                extractNormalized(normalized, "Cédula:\\s*([\\d\\.\\s-]{5,20})", 1),
                extractNormalized(normalized, "Cedula:\\s*([\\d\\.\\s-]{5,20})", 1),
                extractNormalized(normalized, "CC\\s*[:#-]?\\s*([\\d\\.\\s-]{5,20})", 1),
                extractNormalized(ascii, "Identificacion:\\s*([\\d\\.\\s-]{5,20})", 1),
                extractNormalized(ascii, "Numero de documento:\\s*([\\d\\.\\s-]{5,20})", 1),
                extractNormalized(ascii, "Cedula:\\s*([\\d\\.\\s-]{5,20})", 1),
                extractDocumentNumberFromSignature(rawText));

        String cleaned = cleanDocumentNumber(fromKnownLabels);
        if (!cleaned.isBlank()) {
            return cleaned;
        }

        Pattern fallback = Pattern.compile("\\b(\\d{7,12})\\b");
        Matcher matcher = fallback.matcher(normalized);

        if (matcher.find()) {
            return cleanDocumentNumber(matcher.group(1));
        }

        return "";
    }

    private String cleanDocumentNumber(String value) {
        if (value == null) {
            return "";
        }

        return value.replaceAll("\\D", "").trim();
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String removeAccents(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "");
    }

    private String extractNormalized(String text, String regex, int group) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(group) != null ? matcher.group(group).trim() : null;
        }

        return null;
    }

    private String extractPatientNameFromSignature(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return null;
        }

        Pattern pattern = Pattern.compile(
                "Firma y c[eé]dula del Paciente\\s*\\n+\\s*([A-ZÁÉÍÓÚÑ ]{5,})\\s*\\n+\\s*\\d{5,15}",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher matcher = pattern.matcher(rawText);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    private String extractDocumentNumberFromSignature(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return null;
        }

        Pattern pattern = Pattern.compile(
                "Firma y c[eé]dula del Paciente\\s*\\n+\\s*[A-ZÁÉÍÓÚÑ ]{5,}\\s*\\n+\\s*([\\d\\.\\s-]{5,20})",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher matcher = pattern.matcher(rawText);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    private List<String> extractRemissions(String rawText) {
        List<String> remissions = new ArrayList<>();

        if (rawText == null || rawText.isBlank()) {
            return remissions;
        }

        Pattern blockPattern = Pattern.compile(
                "Información de Remisiones\\s*(.*?)\\s*'?CONSENTIMIENTO INFORMADO",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.UNICODE_CASE);
        Matcher blockMatcher = blockPattern.matcher(rawText);

        if (!blockMatcher.find()) {
            return remissions;
        }

        String block = blockMatcher.group(1);
        if (block == null || block.isBlank()) {
            return remissions;
        }

        String[] lines = block.split("\\n");
        for (String line : lines) {
            String value = cleanSingleLine(line);
            if (!value.isBlank()) {
                remissions.add(value);
            }
        }

        return remissions;
    }

    private String cleanSingleLine(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
