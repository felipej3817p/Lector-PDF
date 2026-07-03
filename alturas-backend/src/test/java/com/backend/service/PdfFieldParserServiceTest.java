package com.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

class PdfFieldParserServiceTest {

    private final PdfFieldParserService parserService = new PdfFieldParserService();

    @Test
    void extractsEvaluationDateFromFechaYLugarWithSpanishAbbreviatedMonth() {
        Map<String, Object> fields = parserService.extractFields(
                "Fecha y Lugar: 31 jul. 2025 - TUNJA-BOYACA\nConcepto para trabajo en alturas: APTO");

        assertEquals("2025-07-31", fields.get("fechaEvaluacion"));
        assertEquals("2025-07-31", fields.get("evaluationDate"));
        assertEquals("2025-07-31", fields.get("fechaConcepto"));
    }

    @Test
    void extractsBirthDateAndDoesNotMixExamTypeWithFechaYLugar() {
        Map<String, Object> fields = parserService.extractFields(
                "Tipo de Examen: ALTURAS\n"
                        + "Fecha y Lugar: 28 ago. 2025 - TUNJA-BOYACA\n"
                        + "Fecha Nacimiento: 15 feb. 1988\n"
                        + "Paciente: ALEXANDER PATINO MESA");

        assertEquals("ALTURAS", fields.get("examType"));
        assertEquals("2025-08-28", fields.get("fechaConcepto"));
        assertEquals("2025-08-28", fields.get("fechaEvaluacion"));
        assertEquals("1988-02-15", fields.get("birthDate"));
    }
}
