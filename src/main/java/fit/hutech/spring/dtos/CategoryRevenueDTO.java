package fit.hutech.spring.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryRevenueDTO {
    private String categoryName;
    private Double revenue;
}
