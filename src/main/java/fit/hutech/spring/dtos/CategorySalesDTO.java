package fit.hutech.spring.dtos;

import fit.hutech.spring.entities.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategorySalesDTO {
    private Category category;
    private Long totalSold;
}
