package com.backend.service;

import com.backend.exception.AppException;
import com.backend.exception.ErrorCode;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

@Service
public class PdfTextExtractorService {

    public String extractText(Path filePath) {
        try (PDDocument document = Loader.loadPDF(filePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            throw new AppException(ErrorCode.PDF_TEXT_EXTRACTION_FAILED, "No se pudo extraer texto del PDF.", e);
        }
    }
}
