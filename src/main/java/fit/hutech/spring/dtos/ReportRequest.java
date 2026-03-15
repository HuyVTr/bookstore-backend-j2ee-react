package fit.hutech.spring.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {

    // Loại báo cáo chính: BOOK_SALES, USER_SPENDING, REVENUE_PLATFORM
    private String reportType;
    
    // Định dạng: XLSX, PDF
    private String format;

    // Bộ lọc thời gian (Optional)
    private String dateFrom; // yyyy-MM-dd
    private String dateTo;

    // Giới hạn số lượng (Top N)
    private Integer limit; // ex: 5, 10, 50, 0 (All)

    // Sắp xếp
    private String sortBy; // 'quantity', 'revenue', 'name', 'price'
    private String sortDirection; // 'ASC', 'DESC'

    // Bộ lọc nâng cao (Optional)
    private Double minAmount; // Min spent or Min revenue filtered
    private List<String> platforms; // For User Report: 'FACEBOOK', 'GOOGLE', 'LOCAL'
    private List<String> categories; // For Book Report: 'Novel', 'Science'... (Optional)

    // Cột dữ liệu muốn xuất (Dynamic Columns)
    private List<String> selectedColumns;

    // Thông tin người xuất báo cáo
    private String requesterId;
    private String requesterName;
    private String requesterUsername;
}
