package fit.hutech.spring.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthorSalesDTO {
    private String authorName;
    private Long totalSold;
    private String authorImage; // Placeholder for now, can be updated later
}
