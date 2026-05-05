package com.backend.service;

import com.backend.model.DocumentAnalysis;
import com.backend.model.ManagedDocument;
import com.backend.repository.DocumentAnalysisRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DocumentReportPdfService {

    private final DocumentService documentService;
    private final DocumentAnalysisRepository documentAnalysisRepository;

    public DocumentReportPdfService(DocumentService documentService,
                                    DocumentAnalysisRepository documentAnalysisRepository) {
        this.documentService = documentService;
        this.documentAnalysisRepository = documentAnalysisRepository;
    }

    public byte[] generateReport(String documentId) {
        ManagedDocument document = documentService.findById(documentId);
        DocumentAnalysis analysis = documentAnalysisRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new RuntimeException("No existe análisis guardado para este documento"));

        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            pdf.addPage(page);

            PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font labelFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font textFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream content = new PDPageContentStream(pdf, page)) {
                float margin = 50;
                float y = 760;
                float width = page.getMediaBox().getWidth() - (2 * margin);

                y = writeLine(content, titleFont, 16, margin, y, "REPORTE DE ANALISIS DE DOCUMENTO");
                y -= 10;

                y = writeField(content, labelFont, textFont, margin, y, width, "Archivo:", document.getOriginalFileName());
                y = writeField(content, labelFont, textFont, margin, y, width, "Resultado:", analysis.getResultStatus());
                y = writeField(content, labelFont, textFont, margin, y, width, "Tipo de documento:", document.getDocumentType());
                y = writeField(content, labelFont, textFont, margin, y, width, "Tipo de examen:", document.getExamType());
                y = writeField(content, labelFont, textFont, margin, y, width, "Fecha de analisis:", String.valueOf(analysis.getAnalyzedAt()));

                Map<String, Object> fields = analysis.getExtractedFields();

                y -= 8;
                y = writeLine(content, labelFont, 13, margin, y, "CAMPOS EXTRAIDOS");
                y = writeField(content, labelFont, textFont, margin, y, width, "Paciente:", getValue(fields, "patientName"));
                y = writeField(content, labelFont, textFont, margin, y, width, "Identificacion:", getValue(fields, "documentNumber"));
                y = writeField(content, labelFont, textFont, margin, y, width, "Cargo:", getValue(fields, "position"));
                y = writeField(content, labelFont, textFont, margin, y, width, "Fecha de nacimiento:", getValue(fields, "birthDate"));
                y = writeField(content, labelFont, textFont, margin, y, width, "ARL:", getValue(fields, "arl"));
                y = writeField(content, labelFont, textFont, margin, y, width, "Concepto laboral:", getValue(fields, "laborConcept"));
                y = writeField(content, labelFont, textFont, margin, y, width, "Observaciones:", getValue(fields, "observations"));
                y = writeField(content, labelFont, textFont, margin, y, width, "Programa de vigilancia:", getValue(fields, "surveillanceProgram"));
                y = writeField(content, labelFont, textFont, margin, y, width, "Remisiones:", stringifyReferrals(fields.get("referrals")));
            }

            pdf.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo generar el reporte PDF", e);
        }
    }

    private String getValue(Map<String, Object> fields, String key) {
        Object value = fields != null ? fields.get(key) : null;
        return value != null ? value.toString() : "-";
    }

    private String stringifyReferrals(Object referrals) {
        if (referrals instanceof List<?> list && !list.isEmpty()) {
            List<String> values = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    values.add(item.toString());
                }
            }
            return values.isEmpty() ? "-" : String.join(", ", values);
        }
        return "-";
    }

    private float writeField(PDPageContentStream content,
                             PDType1Font labelFont,
                             PDType1Font textFont,
                             float x,
                             float y,
                             float width,
                             String label,
                             String value) throws IOException {
        String fullText = label + " " + (value != null ? value : "-");
        List<String> lines = wrapText(fullText, textFont, 11, width);

        for (String line : lines) {
            content.beginText();
            content.setFont(textFont, 11);
            content.newLineAtOffset(x, y);
            content.showText(line);
            content.endText();
            y -= 16;
        }

        return y;
    }

    private float writeLine(PDPageContentStream content,
                            PDType1Font font,
                            int size,
                            float x,
                            float y,
                            String text) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
        return y - 20;
    }

    private List<String> wrapText(String text, PDType1Font font, int fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
            float textWidth = font.getStringWidth(testLine) / 1000 * fontSize;

            if (textWidth > maxWidth) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }

        return lines;
    }
}