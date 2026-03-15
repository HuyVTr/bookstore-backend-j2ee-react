package fit.hutech.spring.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RevenueStatsDTO {
    private Double totalRevenue;
    private Double averagePerOrder;
    private Double growthRate;
}
