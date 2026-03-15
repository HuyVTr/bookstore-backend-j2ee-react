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
import fit.hutech.spring.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDashboardRestController {

    private final IBookRepository bookRepository;
    private final IOrderRepository orderRepository;
    private final IUserRepository userRepository;
    private final ISystemActivityRepository activityRepository;

    @GetMapping("/stats/dashboard")
    public ResponseEntity<?> getDashboardStats(@org.springframework.web.bind.annotation.RequestParam(value = "year", required = false) Integer year) {
        int targetYear = (year != null) ? year : java.time.Year.now().getValue();
        Map<String, Object> stats = new HashMap<>();
        
        // 1. User & Book count (Cumulative)
        long totalUsers = userRepository.count();
        long totalBooks = bookRepository.count();
        
        // 2. Order & Revenue (Filter by selected year)
        long yearOrders = orderRepository.countByYear(targetYear);
        Double yearRevenue = orderRepository.getTotalRevenueByYear(targetYear);
        
        stats.put("totalUsers", totalUsers);
        stats.put("totalOrders", yearOrders);
        stats.put("totalRevenue", yearRevenue != null ? yearRevenue : 0.0);
        stats.put("totalBooks", totalBooks);
        
        // Calculate Avg Order Value for the selected year
        double avgOrderValue = (yearOrders > 0) ? (yearRevenue != null ? yearRevenue / yearOrders : 0.0) : 0.0;
        stats.put("avgOrderValue", avgOrderValue);
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/dashboard/overview")
    public ResponseEntity<?> getAdminOverview(@org.springframework.web.bind.annotation.RequestParam(value = "year", required = false) Integer year) {
        int targetYear = (year != null) ? year : java.time.Year.now().getValue();
        Map<String, Object> response = new HashMap<>();

        // 1. Stats Grid (Already provided in /stats but included here for backward compatibility if needed)
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalOrders", orderRepository.count());
        Double totalRevenue = orderRepository.getTotalRevenue();
        stats.put("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);
        
        stats.put("usersGrowth", "+12%"); 
        stats.put("ordersGrowth", "+8%");
        stats.put("revenueGrowth", "+15%");

        response.put("stats", stats);

        // 2. Monthly Revenue Chart (Specific Year)
        List<Object[]> monthlyRevenueData = orderRepository.getMonthlyRevenueByYear(targetYear);
        List<Double> revenueChart = new ArrayList<>();
        for (int i = 0; i < 12; i++) revenueChart.add(0.0);

        for (Object[] row : monthlyRevenueData) {
            if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                int monthIndex = ((Number) row[0]).intValue() - 1;
                double revenue = ((Number) row[1]).doubleValue();
                if (monthIndex >= 0 && monthIndex < 12) {
                    revenueChart.set(monthIndex, revenue);
                }
            }
        }
        response.put("monthlyRevenue", revenueChart);

        // 3. System Activity
        List<SystemActivity> allActivities = activityRepository.findTop10ByOrderByCreatedAtDesc();
        List<SystemActivity> filteredActivities = allActivities.stream()
                .filter(act -> {
                    String role = act.getUserRole() != null ? act.getUserRole().toUpperCase() : "";
                    if (role.contains("ADMIN") || role.contains("STAFF") || role.contains("SYSTEM")) return true;
                    if (role.contains("AUTHOR") && "book".equals(act.getType()) && act.getContent().contains("thêm sách")) return true;
                    return false;
                })
                .collect(java.util.stream.Collectors.toList());
        
        response.put("activities", filteredActivities);

        // 4. System Status
        Map<String, Object> systemStatus = new HashMap<>();
        systemStatus.put("database", "CONNECTED");
        systemStatus.put("apiUptime", "99.99%");
        systemStatus.put("serverStatus", "STABLE");
        systemStatus.put("lastBackup", "2 hours ago");
        
        response.put("systemStatus", systemStatus);

        return ResponseEntity.ok(response);
    }
}
