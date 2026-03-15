package fit.hutech.spring.controllers.rest;

import fit.hutech.spring.entities.User;
import fit.hutech.spring.repositories.IBookRepository;
import fit.hutech.spring.repositories.IOrderRepository;
import fit.hutech.spring.repositories.ReviewRepository;
import fit.hutech.spring.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/author/dashboard")
@RequiredArgsConstructor
public class AuthorDashboardRestController {

    private final IBookRepository bookRepository;
    private final IOrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final UserService userService;

    @GetMapping("/stats")
    public ResponseEntity<?> getAuthorStats() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User currentUser = userService.findByUsername(userDetails.getUsername()).orElse(null);

        if (currentUser == null) {
            return ResponseEntity.status(404).body("User not found");
        }

        Long authorId = currentUser.getId();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBooks", bookRepository.countByCreatedById(authorId));
        
        Long totalSales = orderRepository.getTotalSalesByAuthorId(authorId);
        stats.put("totalSales", totalSales != null ? totalSales : 0);

        Double totalRevenue = orderRepository.getTotalRevenueByAuthorId(authorId);
        stats.put("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);

        Double avgRating = reviewRepository.findAverageRatingByAuthorId(authorId);
        stats.put("averageRating", avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);

        // Chart Data
        int currentYear = java.time.Year.now().getValue();
        stats.put("chartData", orderRepository.getAuthorMonthlySales(authorId, currentYear));

        // Recent Activities
        stats.put("activities", orderRepository.getAuthorRecentActivities(authorId, org.springframework.data.domain.PageRequest.of(0, 10)));

        return ResponseEntity.ok(stats);
    }
}
