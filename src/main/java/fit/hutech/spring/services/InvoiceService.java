package fit.hutech.spring.services;

import fit.hutech.spring.entities.Order;
import fit.hutech.spring.entities.OrderDetail;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class InvoiceService {

    public String generateInvoiceHtml(Order order) {
        StringBuilder html = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        html.append("<!DOCTYPE html>")
            .append("<html lang='vi'>")
            .append("<head>")
            .append("<meta charset='UTF-8'>")
            .append("<style>")
            .append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; color: #333; line-height: 1.6; padding: 40px; }")
            .append(".invoice-box { max-width: 800px; margin: auto; padding: 30px; border: 1px solid #eee; box-shadow: 0 0 10px rgba(0, 0, 0, 0.15); font-size: 16px; background: #fff; }")
            .append(".header { display: flex; justify-content: space-between; border-bottom: 2px solid #000; padding-bottom: 20px; margin-bottom: 20px; }")
            .append(".header h1 { margin: 0; color: #1a73e8; }")
            .append(".info-section { display: flex; justify-content: space-between; margin-bottom: 40px; }")
            .append(".info-section div { width: 45%; }")
            .append("table { width: 100%; border-collapse: collapse; margin-top: 20px; }")
            .append("th { background: #f8f9fa; border-bottom: 2px solid #dee2e6; padding: 12px; text-align: left; }")
            .append("td { padding: 12px; border-bottom: 1px solid #dee2e6; }")
            .append(".total { margin-top: 30px; text-align: right; font-size: 20px; font-weight: bold; color: #1a73e8; }")
            .append(".footer { margin-top: 50px; text-align: center; font-size: 12px; color: #666; border-top: 1px solid #eee; padding-top: 20px; }")
            .append("@media print { .no-print { display: none; } }")
            .append("</style>")
            .append("</head>")
            .append("<body>")
            .append("<div class='invoice-box'>")
            .append("<div class='header'>")
            .append("<div><h1>BOOKSTORE</h1><p>Hệ thống nhà sách trực tuyến uy tín</p></div>")
            .append("<div><p><strong>HÓA ĐƠN BÁN HÀNG</strong></p><p>Mã đơn: #").append(order.getId()).append("</p></div>")
            .append("</div>")
            .append("<div class='info-section'>")
            .append("<div><p><strong>Người đặt:</strong></p><p>").append(order.getUser().getUsername()).append("</p></div>")
            .append("<div><p><strong>Người nhận:</strong></p><p>").append(order.getReceiverName() != null ? order.getReceiverName() : "N/A").append("</p><p>SĐT: ").append(order.getPhoneNumber() != null ? order.getPhoneNumber() : "N/A").append("</p><p>Địa chỉ: ").append(order.getShippingAddress() != null ? order.getShippingAddress() : "N/A").append("</p></div>")
            .append("<div><p><strong>Ngày đặt hàng:</strong></p><p>").append(order.getOrderDate().format(formatter)).append("</p><p><strong>Phương thức thanh toán:</strong></p><p>").append(order.getPaymentMethod()).append("</p></div>")
            .append("</div>")
            .append("<table>")
            .append("<thead><tr><th>Sản phẩm</th><th>Số lượng</th><th>Đơn giá</th><th>Thành tiền</th></tr></thead>")
            .append("<tbody>");

        for (OrderDetail detail : order.getOrderDetails()) {
            double lineTotal = detail.getPrice() * detail.getQuantity();
            html.append("<tr>")
                .append("<td>").append(detail.getBook().getTitle()).append("</td>")
                .append("<td>").append(detail.getQuantity()).append("</td>")
                .append("<td>").append(String.format("%,.0f đ", detail.getPrice())).append("</td>")
                .append("<td>").append(String.format("%,.0f đ", lineTotal)).append("</td>")
                .append("</tr>");
        }

        html.append("</tbody>")
            .append("</table>")
            .append("<div class='total'>TỔNG CỘNG: ").append(String.format("%,.0f đ", order.getTotalPrice())).append("</div>")
            .append("<div class='footer'>")
            .append("<p>Cảm ơn quý khách đã tin tưởng và mua hàng tại BOOKSTORE!</p>")
            .append("<p>Mọi thắc mắc vui lòng liên hệ: 1900 1234 - support@bookstore.vn</p>")
            .append("</div>")
            .append("</div>")
            .append("<div style='text-align: center; margin-top: 20px;' class='no-print'>")
            .append("<button onclick='window.print()' style='padding: 10px 20px; background: #1a73e8; color: white; border: none; border-radius: 5px; cursor: pointer;'>In hóa đơn</button>")
            .append("</div>")
            .append("</body>")
            .append("</html>");

        return html.toString();
    }

    public byte[] generateInvoicePdf(Order order) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, out);

        document.open();
        
        // Header
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Font.BOLD);
        Paragraph header = new Paragraph("INVOICE - BOOKSTORE", headerFont);
        header.setAlignment(Element.ALIGN_CENTER);
        document.add(header);
        document.add(new Paragraph("Order ID: #" + order.getId()));
        document.add(new Paragraph("Order Date: " + order.getOrderDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        document.add(new Paragraph("\n"));

        // Info Section
        document.add(new Paragraph("Customer (Account): " + order.getUser().getUsername()));
        document.add(new Paragraph("Receiver: " + (order.getReceiverName() != null ? order.getReceiverName() : "N/A")));
        document.add(new Paragraph("Phone: " + (order.getPhoneNumber() != null ? order.getPhoneNumber() : "N/A")));
        document.add(new Paragraph("Address: " + (order.getShippingAddress() != null ? order.getShippingAddress() : "N/A")));
        document.add(new Paragraph("Payment Method: " + order.getPaymentMethod()));
        document.add(new Paragraph("\n"));

        // Table
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.addCell("Product");
        table.addCell("Quantity");
        table.addCell("Price");
        table.addCell("Total");

        for (OrderDetail detail : order.getOrderDetails()) {
            table.addCell(detail.getBook().getTitle());
            table.addCell(String.valueOf(detail.getQuantity()));
            table.addCell(String.format("%,.0f", detail.getPrice()));
            table.addCell(String.format("%,.0f", detail.getPrice() * detail.getQuantity()));
        }
        document.add(table);

        // Total
        Paragraph total = new Paragraph("\nGrand Total: " + String.format("%,.0f VND", order.getTotalPrice()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
        total.setAlignment(Element.ALIGN_RIGHT);
        document.add(total);

        document.close();
        return out.toByteArray();
    }

    public byte[] generateInvoiceExcel(Order order) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Invoice");

            // Style for header
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            org.apache.poi.ss.usermodel.Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            // Invoice Header
            Row row0 = sheet.createRow(0);
            row0.createCell(0).setCellValue("BOOKSTORE INVOICE - #" + order.getId());
            
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("Customer (Account):");
            row2.createCell(1).setCellValue(order.getUser().getUsername());
            
            Row row3 = sheet.createRow(3);
            row3.createCell(0).setCellValue("Receiver Name:");
            row3.createCell(1).setCellValue(order.getReceiverName() != null ? order.getReceiverName() : "N/A");

            Row row4 = sheet.createRow(4);
            row4.createCell(0).setCellValue("Phone:");
            row4.createCell(1).setCellValue(order.getPhoneNumber() != null ? order.getPhoneNumber() : "N/A");

            Row row5 = sheet.createRow(5);
            row5.createCell(0).setCellValue("Address:");
            row5.createCell(1).setCellValue(order.getShippingAddress() != null ? order.getShippingAddress() : "N/A");

            Row row6 = sheet.createRow(6);
            row6.createCell(0).setCellValue("Order Date:");
            row6.createCell(1).setCellValue(order.getOrderDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            // Product Table
            Row headerRow = sheet.createRow(8);
            String[] columns = {"Product", "Quantity", "Price", "Subtotal"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 9;
            for (OrderDetail detail : order.getOrderDetails()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(detail.getBook().getTitle());
                row.createCell(1).setCellValue(detail.getQuantity());
                row.createCell(2).setCellValue(detail.getPrice());
                row.createCell(3).setCellValue(detail.getPrice() * detail.getQuantity());
            }

            // Total
            Row totalRow = sheet.createRow(rowIdx + 1);
            totalRow.createCell(2).setCellValue("GRAND TOTAL:");
            totalRow.createCell(3).setCellValue(order.getTotalPrice());

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
