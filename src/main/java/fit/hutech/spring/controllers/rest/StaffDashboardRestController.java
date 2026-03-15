package fit.hutech.spring.controllers.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fit.hutech.spring.entities.SystemActivity;
import fit.hutech.spring.repositories.IBookRepository;
import fit.hutech.spring.repositories.IOrderRepository;
import fit.hutech.spring.repositories.ISystemActivityRepository;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/staff/dashboard")
@RequiredArgsConstructor
public class StaffDashboardRestController {

    private final IBookRepository bookRepository;
    private final IOrderRepository orderRepository;
    private final ISystemActivityRepository activityRepository;

    @GetMapping("/overview")
    public ResponseEntity<?> getDashboardOverview() {
        Map<String, Object> response = new HashMap<>();

        // 1. Stats
        Map<String, Object> stats = new HashMap<>();
        long totalBooks = bookRepository.count();
        long totalOrders = orderRepository.count();
        long lowStock = bookRepository.countByQuantityLessThan(10);
        Double totalRevenue = orderRepository.getTotalRevenue();

        stats.put("totalBooks", totalBooks);
        stats.put("totalOrders", totalOrders);
        stats.put("lowStock", lowStock);
        stats.put("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);

        response.put("stats", stats);

        // 2. Chart Data (12 months)
        int currentYear = java.time.Year.now().getValue();
        List<Object[]> monthlyQueryData = orderRepository.getMonthlyBooksSoldByYear(currentYear);
        List<Long> chartData = new ArrayList<>();
        // Initialize 12 months with 0
        for (int i = 0; i < 12; i++) {
            chartData.add(0L);
        }

        // Fill data from query
        for (Object[] row : monthlyQueryData) {
            if (row[0] != null && row[1] != null) {
                int monthIndex = ((Number) row[0]).intValue() - 1; // 1-12 to 0-11
                long quantity = ((Number) row[1]).longValue();
                if (monthIndex >= 0 && monthIndex < 12) {
                    chartData.set(monthIndex, quantity);
                }
            }
        }
        
        // Find max element for height calculation on frontend
        long maxVal = chartData.stream().max(Long::compareTo).orElse(0L);
        List<Number> finalChartData = new ArrayList<>();
        for (Long val : chartData) {
            if (maxVal == 0) finalChartData.add(0);
            else finalChartData.add((val * 100.0) / maxVal);
        }
        
        Map<String, Object> chartInfo = new HashMap<>();
        chartInfo.put("data", chartData); // raw quantities
        chartInfo.put("percentages", finalChartData); // percentage for bar height
        response.put("chartData", chartInfo);

        // 3. Activity Log
        List<SystemActivity> activities = activityRepository.findTop10ByOrderByCreatedAtDesc();
        List<Map<String, Object>> activityList = new ArrayList<>();
        for (SystemActivity act : activities) {
            Map<String, Object> actMap = new HashMap<>();
            actMap.put("id", act.getId());
            actMap.put("type", act.getType());
            actMap.put("text", act.getContent());
            actMap.put("username", act.getUsername());
            actMap.put("userRole", act.getUserRole());
            actMap.put("createdAt", act.getCreatedAt());
            activityList.add(actMap);
        }
        response.put("recentActivity", activityList);

        return ResponseEntity.ok(response);
    }
}
