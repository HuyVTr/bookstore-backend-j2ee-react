package fit.hutech.spring.controllers.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fit.hutech.spring.dtos.ReportRequest;
import fit.hutech.spring.services.ReportService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class ReportRestController {

    private final ReportService reportService;
    
    @GetMapping("/revenue/stats")
    public ResponseEntity<?> getRevenueStats() {
        return ResponseEntity.ok(reportService.getRevenueStats());
    }

    @GetMapping("/revenue/by-category")
    public ResponseEntity<?> getRevenueByCategory() {
        return ResponseEntity.ok(reportService.getRevenueByCategory());
    }

    @GetMapping("/revenue/by-payment")
    public ResponseEntity<?> getPaymentDistribution() {
        return ResponseEntity.ok(reportService.getPaymentDistribution());
    }

    @GetMapping("/analytics/monthly")
    public ResponseEntity<?> getMonthlyAnalytics() {
        return ResponseEntity.ok(reportService.getMonthlyAnalytics());
    }

    @GetMapping("/analytics/top-books")
    public ResponseEntity<?> getTopSellingBooks() {
        return ResponseEntity.ok(reportService.getTopSellingBooks(5));
    }

    @GetMapping("/top-spenders")
    public ResponseEntity<?> getTopSpenders() {
        return ResponseEntity.ok(reportService.getTopSpenders(5));
    }

    @GetMapping("/platform-stats")
    public ResponseEntity<?> getPlatformStats() {
        return ResponseEntity.ok(reportService.getPlatformStats());
    }
    
    @GetMapping("/export/top-selling")
    public ResponseEntity<InputStreamResource> exportTopSelling() throws IOException {
        ByteArrayInputStream in = reportService.exportTopSellingBooks();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=top-selling-books.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }

    @PostMapping("/export")
    public ResponseEntity<InputStreamResource> exportReport(@RequestBody ReportRequest request) throws IOException {
        ByteArrayInputStream in = reportService.generateReport(request);

        boolean isPdf = "PDF".equalsIgnoreCase(request.getFormat());
        String filename = "report_" + request.getReportType().toLowerCase() + "_" + System.currentTimeMillis();
        String extension = isPdf ? ".pdf" : ".xlsx";
        MediaType mediaType = isPdf ? MediaType.APPLICATION_PDF : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + filename + extension);

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(mediaType)
                .body(new InputStreamResource(in));
    }
}
