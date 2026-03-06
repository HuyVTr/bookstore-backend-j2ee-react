package fit.hutech.spring.controllers.rest;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fit.hutech.spring.entities.Order;
import fit.hutech.spring.entities.User;
import fit.hutech.spring.services.OrderService;
import fit.hutech.spring.services.UserService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderRestController {

    private final OrderService orderService;
    private final UserService userService;

    @GetMapping("/history")
    public ResponseEntity<List<Order>> getOrderHistory() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(401).build();
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User currentUser = userService.findByUsername(userDetails.getUsername()).orElse(null);

        if (currentUser != null) {
            return ResponseEntity.ok(orderService.getOrdersByUserId(currentUser.getId()));
        }

        return ResponseEntity.status(404).build();
    }
}
