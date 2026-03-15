package fit.hutech.spring.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyAnalyticsDTO {
    private int month;
    private Double revenue;
    private Long booksSold;
}
