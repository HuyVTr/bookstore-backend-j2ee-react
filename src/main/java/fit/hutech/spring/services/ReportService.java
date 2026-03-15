package fit.hutech.spring.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import fit.hutech.spring.dtos.BookSalesDTO;
import fit.hutech.spring.repositories.IOrderDetailRepository;

@Service
public class ReportService {

    @Autowired
    private IOrderDetailRepository orderDetailRepository;
    @Autowired
    private fit.hutech.spring.repositories.IOrderRepository orderRepository;
    @Autowired
    private fit.hutech.spring.repositories.IUserRepository userRepository;

    public ByteArrayInputStream generateReport(fit.hutech.spring.dtos.ReportRequest request) throws IOException {
        boolean isPdf = "PDF".equalsIgnoreCase(request.getFormat());
        
        List<String> masterOrder;
        if ("USER_SPENDING".equals(request.getReportType())) {
            masterOrder = List.of("id", "username", "email", "fullname", "provider", "total_spent");
        } else if ("REVENUE_PLATFORM".equals(request.getReportType())) {
            masterOrder = List.of("platform", "count");
        } else {
            // Default BOOK_SALES
            masterOrder = List.of("id", "title", "author", "category", "price", "sold", "revenue");
        }

        // 1. Ensure id is present and mandatory for types that have it
        List<String> selected = request.getSelectedColumns();
        if (selected == null || selected.isEmpty()) {
            selected = new java.util.ArrayList<>(masterOrder);
        } else {
            selected = new java.util.ArrayList<>(selected);
            if (masterOrder.contains("id") && !selected.contains("id")) {
                selected.add(0, "id");
            }
        }

        // 2. Enforce natural order and remove duplicates
        final List<String> finalMaster = masterOrder;
        selected = selected.stream()
                .filter(finalMaster::contains)
                .distinct()
                .sorted(java.util.Comparator.comparingInt(finalMaster::indexOf))
                .collect(java.util.stream.Collectors.toList());
        
        // Re-add id at start if it was filtered out but exists in master
        if (finalMaster.contains("id") && !selected.contains("id")) {
            selected.add(0, "id");
        }
        
        request.setSelectedColumns(selected);

        if ("BOOK_SALES".equals(request.getReportType())) {
            return isPdf ? generateBookSalesPdf(request) : generateBookSalesReport(request);
        } else if ("USER_SPENDING".equals(request.getReportType())) {
            return isPdf ? generateUserSpendingPdf(request) : generateUserSpendingReport(request);
        } else if ("REVENUE_PLATFORM".equals(request.getReportType())) {
            return isPdf ? generatePlatformPdf(request) : generatePlatformReport(request);
        }
        return null;
    }

    // --- PDF GENERATION METHODS ---

    private ByteArrayInputStream generateBookSalesPdf(fit.hutech.spring.dtos.ReportRequest request) {
        List<BookSalesDTO> data = orderDetailRepository.findTopSellingBooks(PageRequest.of(0, 1000));
        
        // Sort
        java.util.Comparator<BookSalesDTO> comparator = java.util.Comparator.comparing(BookSalesDTO::getTotalSold);
        if ("revenue".equalsIgnoreCase(request.getSortBy())) {
            comparator = java.util.Comparator.comparing(dto -> dto.getBook().getPrice() * dto.getTotalSold());
        } else if ("price".equalsIgnoreCase(request.getSortBy())) {
            comparator = java.util.Comparator.comparing(dto -> dto.getBook().getPrice());
        } else if ("title".equalsIgnoreCase(request.getSortBy())) {
            comparator = java.util.Comparator.comparing(dto -> dto.getBook().getTitle());
        }
        if ("DESC".equalsIgnoreCase(request.getSortDirection())) comparator = comparator.reversed();
        data.sort(comparator);

        // Limit
        if (request.getLimit() != null && request.getLimit() > 0 && request.getLimit() < data.size()) {
            data = data.subList(0, request.getLimit());
        }
        
        // Ensure we use a stable reference for the columns
        final List<String> columnsToExport = request.getSelectedColumns();
        
        return generatePdf(data, columnsToExport, "Báo cáo Doanh số sách", request, (table, item, font) -> {
            for (String col : columnsToExport) {
                String val = "-";
                switch (col) {
                    case "id": val = String.valueOf(item.getBook().getId()); break;
                    case "title": val = item.getBook().getTitle(); break;
                    case "author": val = item.getBook().getAuthor() != null ? item.getBook().getAuthor() : "-"; break;
                    case "category": val = (item.getBook().getCategory() != null ? item.getBook().getCategory().getName() : "-"); break;
                    case "price": val = String.format("%,.0fđ", item.getBook().getPrice()); break;
                    case "sold": val = String.valueOf(item.getTotalSold()); break;
                    case "revenue": val = String.format("%,.0fđ", item.getTotalSold() * item.getBook().getPrice()); break;
                }
                com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(val, font));
                cell.setPadding(5);
                table.addCell(cell);
            }
        });
    }

    private ByteArrayInputStream generateUserSpendingPdf(fit.hutech.spring.dtos.ReportRequest request) {
        List<fit.hutech.spring.dtos.UserSpendingDTO> data = orderRepository.findTopSpenders(PageRequest.of(0, 1000));
        
        // Sort
        java.util.Comparator<fit.hutech.spring.dtos.UserSpendingDTO> comparator = java.util.Comparator.comparing(fit.hutech.spring.dtos.UserSpendingDTO::getTotalSpent);
        if ("username".equalsIgnoreCase(request.getSortBy())) {
            comparator = java.util.Comparator.comparing(dto -> dto.getUser().getUsername());
        } else if ("fullname".equalsIgnoreCase(request.getSortBy())) {
            comparator = java.util.Comparator.comparing(dto -> dto.getUser().getFullName() != null ? dto.getUser().getFullName() : "");
        }
        if ("DESC".equalsIgnoreCase(request.getSortDirection())) comparator = comparator.reversed();
        data.sort(comparator);

        if (request.getLimit() != null && request.getLimit() > 0 && request.getLimit() < data.size()) {
            data = data.subList(0, request.getLimit());
        }

        final List<String> cols = request.getSelectedColumns();
        return generatePdf(data, cols, "Báo cáo Chi tiêu khách hàng", request, (table, item, font) -> {
            for (String col : cols) {
                String val = "-";
                switch (col) {
                    case "id": val = String.valueOf(item.getUser().getId()); break;
                    case "username": val = item.getUser().getUsername(); break;
                    case "email": val = item.getUser().getEmail(); break;
                    case "fullname": val = (item.getUser().getFullName() != null ? item.getUser().getFullName() : ""); break;
                    case "provider": val = (item.getUser().getProvider() != null ? item.getUser().getProvider() : "LOCAL"); break;
                    case "total_spent": val = String.format("%,.0fđ", item.getTotalSpent()); break;
                }
                com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(val, font));
                cell.setPadding(5);
                table.addCell(cell);
            }
        });
    }

    private ByteArrayInputStream generatePlatformPdf(fit.hutech.spring.dtos.ReportRequest request) {
        List<fit.hutech.spring.dtos.PlatformStatsDTO> data = userRepository.countUsersByPlatform();
        
        // Sort
        java.util.Comparator<fit.hutech.spring.dtos.PlatformStatsDTO> comparator = java.util.Comparator.comparing(fit.hutech.spring.dtos.PlatformStatsDTO::getCount);
        if ("platform".equalsIgnoreCase(request.getSortBy())) {
            comparator = java.util.Comparator.comparing(fit.hutech.spring.dtos.PlatformStatsDTO::getProvider);
        }
        if ("DESC".equalsIgnoreCase(request.getSortDirection())) comparator = comparator.reversed();
        data.sort(comparator);

        if (request.getLimit() != null && request.getLimit() > 0 && request.getLimit() < data.size()) {
            data = data.subList(0, request.getLimit());
        }

        final List<String> cols = request.getSelectedColumns() != null ? request.getSelectedColumns() : List.of("platform", "count");
        
        return generatePdf(data, cols, "Thống kê nguồn người dùng", request, (table, item, font) -> {
            for (String col : cols) {
                String val = "-";
                switch (col) {
                    case "platform": val = item.getProvider(); break;
                    case "count": val = String.valueOf(item.getCount()); break;
                }
                com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(val, font));
                cell.setPadding(5);
                table.addCell(cell);
            }
        });
    }
    private <T> ByteArrayInputStream generatePdf(List<T> data, List<String> columns, String title, fit.hutech.spring.dtos.ReportRequest request, PdfRowFiller<T> filler) {
        com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();
            
            // Try to load a Vietnamese compatible font (Arial from Windows)
            com.lowagie.text.pdf.BaseFont baseFont;
            try {
                baseFont = com.lowagie.text.pdf.BaseFont.createFont("C:\\Windows\\Fonts\\Arial.ttf", 
                           com.lowagie.text.pdf.BaseFont.IDENTITY_H, com.lowagie.text.pdf.BaseFont.EMBEDDED);
            } catch (Exception e) {
                baseFont = com.lowagie.text.pdf.BaseFont.createFont(com.lowagie.text.pdf.BaseFont.HELVETICA, 
                           com.lowagie.text.pdf.BaseFont.WINANSI, com.lowagie.text.pdf.BaseFont.NOT_EMBEDDED);
            }

            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(baseFont, 18, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font headFont = new com.lowagie.text.Font(baseFont, 11, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font dataFont = new com.lowagie.text.Font(baseFont, 9, com.lowagie.text.Font.NORMAL);
            com.lowagie.text.Font metaFont = new com.lowagie.text.Font(baseFont, 10, com.lowagie.text.Font.ITALIC);

            // Add Title
            com.lowagie.text.Paragraph p = new com.lowagie.text.Paragraph(title.toUpperCase(), titleFont);
            p.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            p.setSpacingAfter(10);
            document.add(p);

            // Add Metadata (Requester Info)
            String exportTime = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));
            String requesterInfo = "Người xuất: " + (request.getRequesterName() != null ? request.getRequesterName() : "N/A") +
                                   " (" + (request.getRequesterUsername() != null ? request.getRequesterUsername() : "guest") + ")" +
                                   " | Thời điểm: " + exportTime;
            com.lowagie.text.Paragraph metaP = new com.lowagie.text.Paragraph(requesterInfo, metaFont);
            metaP.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            metaP.setSpacingAfter(20);
            document.add(metaP);
 
            // Create Table
            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(columns.size());
            table.setWidthPercentage(100);

            // Header
            for (String col : columns) {
                com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(getColumnLabel(col), headFont));
                cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
                cell.setPadding(6);
                table.addCell(cell);
            }

            // Data Rows
            for (T item : data) {
                filler.fill(table, item, dataFont);
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    @FunctionalInterface
    interface PdfRowFiller<T> {
        void fill(com.lowagie.text.pdf.PdfPTable table, T item, com.lowagie.text.Font font);
    }

    // --- 1. BOOK SALES REPORT ---
    private ByteArrayInputStream generateBookSalesReport(fit.hutech.spring.dtos.ReportRequest request)
            throws IOException {
        List<BookSalesDTO> data = orderDetailRepository.findTopSellingBooks(PageRequest.of(0, 1000));

        // Sort
        java.util.Comparator<BookSalesDTO> comparator = java.util.Comparator.comparing(BookSalesDTO::getTotalSold); // Default (sold/quantity)
        if ("revenue".equalsIgnoreCase(request.getSortBy())) {
            comparator = java.util.Comparator.comparing(dto -> dto.getBook().getPrice() * dto.getTotalSold());
        } else if ("price".equalsIgnoreCase(request.getSortBy())) {
            comparator = java.util.Comparator.comparing(dto -> dto.getBook().getPrice());
        } else if ("title".equalsIgnoreCase(request.getSortBy()) || "name".equalsIgnoreCase(request.getSortBy())) {
            comparator = java.util.Comparator.comparing(dto -> dto.getBook().getTitle());
        }

        if ("DESC".equalsIgnoreCase(request.getSortDirection())) {
            comparator = comparator.reversed();
        }
        data.sort(comparator);

        if (request.getLimit() != null && request.getLimit() > 0 && request.getLimit() < data.size()) {
            data = data.subList(0, request.getLimit());
        }

        // Generate Excel
        final List<String> cols = request.getSelectedColumns();
        return generateExcel(data, cols, "Book Sales", request, (cell, item, col) -> {
            switch (col) {
                case "id":
                    cell.setCellValue(item.getBook().getId());
                    break;
                case "title":
                    cell.setCellValue(item.getBook().getTitle());
                    break;
                case "author":
                    cell.setCellValue(item.getBook().getAuthor() != null ? item.getBook().getAuthor() : "-");
                    break;
                case "category":
                    cell.setCellValue(item.getBook().getCategory() != null ? item.getBook().getCategory().getName() : "-");
                    break;
                case "price":
                    cell.setCellValue(item.getBook().getPrice());
                    break;
                case "sold":
                    cell.setCellValue(item.getTotalSold());
                    break;
                case "revenue":
                    cell.setCellValue(item.getTotalSold() * item.getBook().getPrice());
                    break;
            }
        });
    }

    // --- 2. USER SPENDING REPORT ---
    private ByteArrayInputStream generateUserSpendingReport(fit.hutech.spring.dtos.ReportRequest request)
            throws IOException {
        List<fit.hutech.spring.dtos.UserSpendingDTO> data = orderRepository.findTopSpenders(PageRequest.of(0, 1000));

        // Sort
        java.util.Comparator<fit.hutech.spring.dtos.UserSpendingDTO> comparator = java.util.Comparator
                .comparing(fit.hutech.spring.dtos.UserSpendingDTO::getTotalSpent); // Default (total_spent)
        
        if ("username".equalsIgnoreCase(request.getSortBy()) || "name".equalsIgnoreCase(request.getSortBy())) {
            comparator = java.util.Comparator.comparing(dto -> dto.getUser().getUsername());
        } else if ("fullname".equalsIgnoreCase(request.getSortBy())) {
            comparator = java.util.Comparator.comparing(dto -> dto.getUser().getFullName() != null ? dto.getUser().getFullName() : "");
        }

        if ("DESC".equalsIgnoreCase(request.getSortDirection())) {
            comparator = comparator.reversed();
        }
        data.sort(comparator);

        if (request.getLimit() != null && request.getLimit() > 0 && request.getLimit() < data.size()) {
            data = data.subList(0, request.getLimit());
        }

        final List<String> cols = request.getSelectedColumns();
        return generateExcel(data, cols, "User Spending", request, (cell, item, col) -> {
            switch (col) {
                case "id":
                    cell.setCellValue(item.getUser().getId());
                    break;
                case "username":
                    cell.setCellValue(item.getUser().getUsername());
                    break;
                case "email":
                    cell.setCellValue(item.getUser().getEmail());
                    break;
                case "fullname":
                    cell.setCellValue(item.getUser().getFullName() != null ? item.getUser().getFullName() : "-");
                    break;
                case "provider":
                    cell.setCellValue(item.getUser().getProvider() != null ? item.getUser().getProvider() : "LOCAL");
                    break;
                case "total_spent":
                    cell.setCellValue(item.getTotalSpent());
                    break;
            }
        });
    }

    // --- 3. PLATFORM STATS REPORT ---
    private ByteArrayInputStream generatePlatformReport(fit.hutech.spring.dtos.ReportRequest request)
            throws IOException {
        List<fit.hutech.spring.dtos.PlatformStatsDTO> data = userRepository.countUsersByPlatform();

        // Sort
        java.util.Comparator<fit.hutech.spring.dtos.PlatformStatsDTO> comparator = java.util.Comparator.comparing(fit.hutech.spring.dtos.PlatformStatsDTO::getCount); // Default (count)
        if ("platform".equalsIgnoreCase(request.getSortBy())) {
            comparator = java.util.Comparator.comparing(fit.hutech.spring.dtos.PlatformStatsDTO::getProvider);
        }

        if ("DESC".equalsIgnoreCase(request.getSortDirection())) {
            comparator = comparator.reversed();
        }
        data.sort(comparator);

        final List<String> cols = request.getSelectedColumns() != null ? request.getSelectedColumns() : List.of("platform", "count");

        return generateExcel(data, cols, "Platform Statistics", request, (cell, item, col) -> {
            switch (col) {
                case "platform":
                    cell.setCellValue(item.getProvider());
                    break;
                case "count":
                    cell.setCellValue(item.getCount());
                    break;
            }
        });
    }

    // --- GENERIC EXCEL GENERATOR HELPER ---
    private <T> ByteArrayInputStream generateExcel(List<T> data, List<String> columns, String sheetName,
            fit.hutech.spring.dtos.ReportRequest request, RowDataFiller<T> filler) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);

            // Font & Styles
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.INDIGO.getIndex());

            org.apache.poi.ss.usermodel.Font metaFont = workbook.createFont();
            metaFont.setItalic(true);
            metaFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_50_PERCENT.getIndex());

            org.apache.poi.ss.usermodel.Font headFont = workbook.createFont();
            headFont.setBold(true);
            headFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());

            org.apache.poi.ss.usermodel.CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            org.apache.poi.ss.usermodel.CellStyle metaStyle = workbook.createCellStyle();
            metaStyle.setFont(metaFont);

            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headFont);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.INDIGO.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.MEDIUM);

            org.apache.poi.ss.usermodel.CellStyle rowStyle = workbook.createCellStyle();
            rowStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            rowStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            rowStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            rowStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);

            // 1. Report Title
            Row r0 = sheet.createRow(0);
            r0.createCell(0).setCellValue("BÁO CÁO HỆ THỐNG - " + sheetName.toUpperCase());
            r0.getCell(0).setCellStyle(titleStyle);

            // 2. Requester Metadata
            String exportTime = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));
            
            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("Người xuất:");
            r2.createCell(1).setCellValue(request.getRequesterName() != null ? request.getRequesterName() : "N/A");
            r2.getCell(0).setCellStyle(metaStyle);

            Row r3 = sheet.createRow(3);
            r3.createCell(0).setCellValue("Tài khoản (ID):");
            String accInfo = (request.getRequesterUsername() != null ? request.getRequesterUsername() : "guest") + 
                             " (#" + (request.getRequesterId() != null ? request.getRequesterId() : "0") + ")";
            r3.createCell(1).setCellValue(accInfo);
            r3.getCell(0).setCellStyle(metaStyle);

            Row r4 = sheet.createRow(4);
            r4.createCell(0).setCellValue("Thời điểm xuất:");
            r4.createCell(1).setCellValue(exportTime);
            r4.getCell(0).setCellStyle(metaStyle);

            // 3. Table Headers
            int startRow = 6;
            Row headerRow = sheet.createRow(startRow);
            if (columns == null || columns.isEmpty()) columns = List.of("id");

            for (int i = 0; i < columns.size(); i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(getColumnLabel(columns.get(i)));
                cell.setCellStyle(headerStyle);
            }

            // 4. Data Rows
            int rowIdx = startRow + 1;
            for (T item : data) {
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < columns.size(); i++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.createCell(i);
                    cell.setCellStyle(rowStyle);
                    filler.fill(cell, item, columns.get(i));
                }
            }

            // Auto-size columns
            for (int i = 0; i < columns.size(); i++) {
                sheet.autoSizeColumn(i);
            }
                
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    @FunctionalInterface
    interface RowDataFiller<T> {
        void fill(org.apache.poi.ss.usermodel.Cell cell, T item, String column);
    }

    private String getColumnLabel(String col) {
        switch (col) {
            case "id":
                return "ID";
            case "title":
                return "Tên Sách";
            case "author":
                return "Tác Giả";
            case "category":
                return "Thể Loại";
            case "price":
                return "Giá Bán";
            case "sold":
                return "Số Lượng Bán";
            case "revenue":
                return "Doanh Thu";
            case "username":
                return "Tên Đăng Nhập";
            case "email":
                return "Email";
            case "fullname":
                return "Họ Tên";
            case "provider":
                return "Nền Tảng";
            case "total_spent":
                return "Tổng Chi Tiêu";
            case "platform":
                return "Nền Tảng";
            case "count":
                return "Số Lượng User";
            default:
                return col;
        }
    }

    public fit.hutech.spring.dtos.RevenueStatsDTO getRevenueStats() {
        Double totalRevenue = orderRepository.getTotalRevenue();
        long completedOrders = orderRepository.countByStatus("COMPLETED");
        Double avgOrderValue = completedOrders > 0 ? totalRevenue / completedOrders : 0.0;

        // Calculate growth (this month vs last month)
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int currentMonth = cal.get(java.util.Calendar.MONTH) + 1;
        int currentYear = cal.get(java.util.Calendar.YEAR);
        
        cal.add(java.util.Calendar.MONTH, -1);
        int lastMonth = cal.get(java.util.Calendar.MONTH) + 1;
        int lastYear = cal.get(java.util.Calendar.YEAR);

        Double currentMonthRevenue = orderRepository.getRevenueByMonthAndYear(currentMonth, currentYear);
        Double lastMonthRevenue = orderRepository.getRevenueByMonthAndYear(lastMonth, lastYear);
        
        if (currentMonthRevenue == null) currentMonthRevenue = 0.0;
        if (lastMonthRevenue == null) lastMonthRevenue = 0.0;

        Double growthRate = 0.0;
        if (lastMonthRevenue > 0) {
            growthRate = ((currentMonthRevenue - lastMonthRevenue) / lastMonthRevenue) * 100;
        } else if (currentMonthRevenue > 0) {
            growthRate = 100.0;
        }

        return new fit.hutech.spring.dtos.RevenueStatsDTO(totalRevenue, avgOrderValue, growthRate);
    }

    public List<fit.hutech.spring.dtos.CategoryRevenueDTO> getRevenueByCategory() {
        List<Object[]> results = orderDetailRepository.getRevenueByCategory();
        return results.stream()
                .map(res -> new fit.hutech.spring.dtos.CategoryRevenueDTO((String) res[0], (Double) res[1]))
                .collect(java.util.stream.Collectors.toList());
    }

    public List<fit.hutech.spring.dtos.PaymentMethodDTO> getPaymentDistribution() {
        List<Object[]> results = orderRepository.countByPaymentMethod();
        long totalOrders = results.stream().mapToLong(res -> (long) res[1]).sum();
        
        return results.stream()
                .map(res -> {
                    long count = (long) res[1];
                    double percentage = totalOrders > 0 ? (count * 100.0 / totalOrders) : 0.0;
                    return new fit.hutech.spring.dtos.PaymentMethodDTO((String) res[0], count, percentage);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    public List<fit.hutech.spring.dtos.MonthlyAnalyticsDTO> getMonthlyAnalytics() {
        int currentYear = java.time.Year.now().getValue();
        List<Object[]> revenueResults = orderRepository.getMonthlyRevenueByYear(currentYear);
        List<Object[]> soldResults = orderRepository.getMonthlyBooksSoldByYear(currentYear);
        
        java.util.Map<Integer, fit.hutech.spring.dtos.MonthlyAnalyticsDTO> resultMap = new java.util.TreeMap<>();
        
        for (Object[] res : revenueResults) {
            int month = ((Number) res[0]).intValue();
            double revenue = ((Number) res[1]).doubleValue();
            resultMap.put(month, new fit.hutech.spring.dtos.MonthlyAnalyticsDTO(month, revenue, 0L));
        }
        
        for (Object[] res : soldResults) {
            int month = ((Number) res[0]).intValue();
            long sold = ((Number) res[1]).longValue();
            if (resultMap.containsKey(month)) {
                resultMap.get(month).setBooksSold(sold);
            } else {
                resultMap.put(month, new fit.hutech.spring.dtos.MonthlyAnalyticsDTO(month, 0.0, sold));
            }
        }
        
        return new java.util.ArrayList<>(resultMap.values());
    }

    public List<fit.hutech.spring.dtos.BookSalesDTO> getTopSellingBooks(int limit) {
        return orderDetailRepository.findTopSellingBooks(PageRequest.of(0, limit));
    }

    public List<fit.hutech.spring.dtos.UserSpendingDTO> getTopSpenders(int limit) {
        return orderRepository.findTopSpenders(PageRequest.of(0, limit));
    }

    public List<fit.hutech.spring.dtos.PlatformStatsDTO> getPlatformStats() {
        return userRepository.countUsersByPlatform();
    }

    // Backup method for backward compatibility
    public ByteArrayInputStream exportTopSellingBooks() throws IOException {
        fit.hutech.spring.dtos.ReportRequest req = new fit.hutech.spring.dtos.ReportRequest();
        req.setReportType("BOOK_SALES");
        req.setLimit(5);
        req.setSortBy("quantity");
        req.setSortDirection("DESC");
        req.setSelectedColumns(List.of("id", "title", "category", "price", "sold"));
        return generateReport(req);
    }
}
