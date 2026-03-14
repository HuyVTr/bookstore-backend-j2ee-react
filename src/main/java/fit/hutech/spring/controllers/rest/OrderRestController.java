package fit.hutech.spring.controllers.rest;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fit.hutech.spring.entities.Order;
import fit.hutech.spring.entities.User;
import fit.hutech.spring.services.InvoiceService;
import fit.hutech.spring.services.OrderService;
import fit.hutech.spring.services.UserService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderRestController {

    private final OrderService orderService;
    private final UserService userService;
    private final InvoiceService invoiceService;

    @GetMapping("/history")
    public ResponseEntity<List<Order>> getOrderHistory() {
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            return ResponseEntity.ok(orderService.getOrdersByUserId(currentUser.getId()));
        }
        return ResponseEntity.status(401).build();
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return ResponseEntity.status(401).body("Unauthorized");

        Order order = orderService.getOrderById(orderId).orElse(null);
        if (order == null) return ResponseEntity.status(404).body("Order not found");

        if (!order.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).body("You can only cancel your own orders");
        }

        if (!"PENDING".equalsIgnoreCase(order.getStatus())) {
            return ResponseEntity.badRequest().body("Only PENDING orders can be cancelled");
        }

        orderService.updateOrderStatus(orderId, "CANCELLED");
        return ResponseEntity.ok("Order cancelled successfully");
    }

    @GetMapping(value = "/{orderId}/invoice", produces = org.springframework.http.MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getInvoice(@PathVariable Long orderId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return ResponseEntity.status(401).build();

        Order order = orderService.getOrderById(orderId).orElse(null);
        if (order == null) return ResponseEntity.status(404).build();

        if (!order.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).build();
        }

        if (!"COMPLETED".equalsIgnoreCase(order.getStatus())) {
            return ResponseEntity.badRequest().body("Invoice is only available for COMPLETED orders");
        }

        return ResponseEntity.ok(invoiceService.generateInvoiceHtml(order));
    }

    @GetMapping("/{orderId}/invoice/pdf")
    public ResponseEntity<byte[]> getInvoicePdf(@PathVariable Long orderId) throws java.io.IOException {
        User currentUser = getCurrentUser();
        if (currentUser == null) return ResponseEntity.status(401).build();

        Order order = orderService.getOrderById(orderId).orElse(null);
        if (order == null) return ResponseEntity.status(404).build();
        if (!order.getUser().getId().equals(currentUser.getId())) return ResponseEntity.status(403).build();
        if (!"COMPLETED".equalsIgnoreCase(order.getStatus())) return ResponseEntity.badRequest().build();

        byte[] pdf = invoiceService.generateInvoicePdf(order);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice_" + orderId + ".pdf");
        return new ResponseEntity<>(pdf, headers, org.springframework.http.HttpStatus.OK);
    }

    @GetMapping("/{orderId}/invoice/excel")
    public ResponseEntity<byte[]> getInvoiceExcel(@PathVariable Long orderId) throws java.io.IOException {
        User currentUser = getCurrentUser();
        if (currentUser == null) return ResponseEntity.status(401).build();

        Order order = orderService.getOrderById(orderId).orElse(null);
        if (order == null) return ResponseEntity.status(404).build();
        if (!order.getUser().getId().equals(currentUser.getId())) return ResponseEntity.status(403).build();
        if (!"COMPLETED".equalsIgnoreCase(order.getStatus())) return ResponseEntity.badRequest().build();

        byte[] excel = invoiceService.generateInvoiceExcel(order);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "invoice_" + orderId + ".xlsx");
        return new ResponseEntity<>(excel, headers, org.springframework.http.HttpStatus.OK);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            return userService.findByUsername(userDetails.getUsername()).orElse(null);
        }
        return null;
    }
}
