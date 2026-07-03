package com.backend.controller;

import com.backend.service.ReportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /*
     * Excel conceptos de aptitud.
     *
     * mode=latest  -> último concepto por trabajador.
     * mode=history -> histórico completo.
     */
    @GetMapping("/aptitude-excel")
    public ResponseEntity<byte[]> downloadAptitudeExcel(
            @RequestParam Map<String, String> filters
    ) {
        byte[] content = reportService.generateAptitudeExcel(filters);

        String mode = String.valueOf(filters.getOrDefault("mode", "latest"));
        String suffix = "history".equalsIgnoreCase(mode) ? "historico" : "ultimo";
        String filename = "reporte_conceptos_aptitud_alturas_" + suffix + "_" + LocalDate.now() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(filename))
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    /*
     * CSV trabajadores / plano para Ministerio u otros sistemas.
     *
     * Orden:
     * Tipo documento;Número documento;Primer nombre;Segundo nombre;Primer apellido;Segundo apellido;Género;País;Fecha nacimiento;Profesión;Área;Cargo;Sector;Empresa;ARL
     */
    @GetMapping("/ministry-csv")
    public ResponseEntity<byte[]> downloadMinistryCsv(
            @RequestParam Map<String, String> filters
    ) {
        byte[] content = reportService.generateMinistryCsv(filters);

        String filename = "trabajadores_ministerio_" + LocalDate.now() + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(filename))
                .contentType(MediaType.parseMediaType("text/csv; charset=ISO-8859-1"))
                .body(content);
    }

    /*
     * Alias más claro para frontend.
     */
    @GetMapping("/workers-csv")
    public ResponseEntity<byte[]> downloadWorkersCsv(
            @RequestParam Map<String, String> filters
    ) {
        return downloadMinistryCsv(filters);
    }

    private String contentDisposition(String filename) {
        return ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build()
                .toString();
    }
}