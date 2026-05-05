package com.backend.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfFieldParserService {

    public Map<String, Object> extractFields(String text) {
        Map<String, Object> fields = new LinkedHashMap<>();

        String raw = text != null ? text.replace("\r", "") : "";
        String normalized = normalizeText(raw);

        String patientName = firstNonBlank(
                extractNormalized(normalized, "Paciente:\\s*(.*?)\\s*Teléfono", 1),
                extractPatientNameFromSignature(raw)
        );

        String documentNumber = firstNonBlank(
                extractNormalized(normalized, "Identificación:\\s*(\\d{5,15})", 1),
                extractDocumentNumberFromSignature(raw)
        );

        String position = extractNormalized(normalized, "Cargo:\\s*(.*?)\\s*Paciente:", 1);

        String examType = extractNormalized(
                normalized,
                "Tipo de Examen:\\s*(.*?)\\s*(?:Estado Civil:|Fecha Nacimiento:)",
                1
        );

        String birthDate = extractNormalized(normalized, "Fecha Nacimiento:\\s*(\\d{2}/\\d{2}/\\d{4})", 1);

        String arl = cleanSingleLine(extractNormalized(normalized, "ARL:\\s*(.*?)\\s*AFP:", 1));

        String laborConcept = extractNormalized(
                normalized,
                "CONCEPTO LABORAL\\s*(.*?)\\s*Observaciones:",
                1
        );

        String observations = extractNormalized(
                normalized,
                "Observaciones:\\s*(.*?)\\s*Tipo de Restricción",
                1
        );

        String surveillanceProgram = extractNormalized(
                normalized,
                "Ingresar al Programa de Vigilancia Epidemiológica o Programa de Prevención y Promoción\\s*(.*?)\\s*Información de Remisiones",
                1
        );

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
        fields.put("referrals", referrals);

        return fields;
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

    private String extractNormalized(String text, String regex, int group) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
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
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        );
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
                "Firma y c[eé]dula del Paciente\\s*\\n+\\s*[A-ZÁÉÍÓÚÑ ]{5,}\\s*\\n+\\s*(\\d{5,15})",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        );
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
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.UNICODE_CASE
        );
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