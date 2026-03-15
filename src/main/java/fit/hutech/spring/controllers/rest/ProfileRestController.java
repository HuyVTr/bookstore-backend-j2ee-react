package fit.hutech.spring.controllers.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import fit.hutech.spring.entities.User;
import fit.hutech.spring.services.UserService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileRestController {

    private final UserService userService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final fit.hutech.spring.repositories.IBookRepository bookRepository;
    private final fit.hutech.spring.repositories.IOrderRepository orderRepository;

    @GetMapping
    public ResponseEntity<?> getProfile() {
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

        return ResponseEntity.ok(createProfileResponse(currentUser));
    }

    private java.util.Map<String, Object> createProfileResponse(User currentUser) {
        // Calculate stats
        long booksCount = bookRepository.countByCreatedById(currentUser.getId());
        long ordersCount = orderRepository.countByProcessedById(currentUser.getId());

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("user", currentUser); // Wrapper user object
        // Direct properties for easy access
        response.put("id", currentUser.getId());
        response.put("username", currentUser.getUsername());
        response.put("email", currentUser.getEmail());
        response.put("fullName", currentUser.getFullName());
        response.put("phone", currentUser.getPhone());
        response.put("address", currentUser.getAddress());
        response.put("avatarPath", currentUser.getAvatarPath());
        response.put("bio", currentUser.getBio());
        response.put("roles", currentUser.getRoles());
        
        // Add Stats
        response.put("booksCount", booksCount);
        response.put("ordersCount", ordersCount);
        
        // Add Author specific stats
        Long authorSales = orderRepository.getTotalSalesByAuthorId(currentUser.getId());
        Double authorRevenue = orderRepository.getTotalRevenueByAuthorId(currentUser.getId());
        response.put("totalAuthorSales", authorSales != null ? authorSales : 0);
        response.put("totalAuthorRevenue", authorRevenue != null ? authorRevenue : 0.0);

        return response;
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody User updatedUser) {
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

        // Kiểm tra số điện thoại trùng lặp (nếu có thay đổi)
        if (updatedUser.getPhone() != null && !updatedUser.getPhone().equals(currentUser.getPhone())) {
            if (userService.findByPhone(updatedUser.getPhone()).isPresent()) {
                return ResponseEntity.badRequest().body("Số điện thoại đã được sử dụng bởi người dùng khác!");
            }
        }

        currentUser.setFullName(updatedUser.getFullName());
        currentUser.setPhone(updatedUser.getPhone());
        currentUser.setAddress(updatedUser.getAddress());
        currentUser.setBio(updatedUser.getBio());

        userService.save(currentUser);
        return ResponseEntity.ok(createProfileResponse(currentUser));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody java.util.Map<String, String> request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User currentUser = userService.findByUsername(userDetails.getUsername()).orElse(null);

        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");

        if (!passwordEncoder.matches(oldPassword, currentUser.getPassword())) {
            return ResponseEntity.badRequest().body("Mật khẩu hiện tại không chính xác!");
        }

        currentUser.setPassword(passwordEncoder.encode(newPassword));
        userService.save(currentUser);

        return ResponseEntity.ok("Đổi mật khẩu thành công!");
    }

    @PostMapping("/avatar")
    public ResponseEntity<?> updateAvatar(@RequestParam("file") MultipartFile file) {
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

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("Vui lòng chọn ảnh!");
        }

        try {
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path uploadPath = Paths.get("uploads");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Lưu đường dẫn ảnh vào DB
            currentUser.setAvatarPath("/images/" + fileName);
            userService.save(currentUser);

            return ResponseEntity.ok(currentUser);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Lỗi khi tải ảnh lên: " + e.getMessage());
        }
    }
}
