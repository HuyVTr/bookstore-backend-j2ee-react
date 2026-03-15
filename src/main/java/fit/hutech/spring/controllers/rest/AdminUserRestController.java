package fit.hutech.spring.controllers.rest;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import fit.hutech.spring.entities.Role;
import fit.hutech.spring.entities.User;
import fit.hutech.spring.repositories.IRoleRepository;
import fit.hutech.spring.repositories.IUserRepository;
import fit.hutech.spring.services.UserService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserRestController {

    private final UserService userService;
    private final IRoleRepository roleRepository;
    private final IUserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final fit.hutech.spring.services.SystemActivityService activityService;

    private User getCurrentUser() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    private boolean isSuperAdmin(User user) {
        if (user == null) return false;
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"));
        if (!isAdmin) return false;
        
        // Super Admin là Admin có ID nhỏ nhất hệ thống
        Long minAdminId = userRepository.findAll().stream()
            .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN")))
            .mapToLong(User::getId)
            .min()
            .orElse(Long.MAX_VALUE);
            
        return user.getId().equals(minAdminId);
    }

    private boolean canManage(User actor, User target) {
        if (isSuperAdmin(actor)) return true; // Super Admin có toàn quyền
        
        boolean targetIsAdmin = target.getRoles().stream()
            .anyMatch(r -> r.getName().equals("ROLE_ADMIN"));
            
        if (targetIsAdmin) return false; // Admin thường không thể động vào Admin khác hoặc Super Admin
        
        return true; // Admin thường có thể quản lý các role khác (USER, STAFF, AUTHOR)
    }

    @GetMapping
    public ResponseEntity<List<User>> listUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/roles")
    public ResponseEntity<List<Role>> listAllRoles() {
        return ResponseEntity.ok(roleRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<?> createUser(@jakarta.validation.Valid @RequestBody User user) {
        User actor = getCurrentUser();
        // Chỉ Admin mới được tạo user, và Admin mới tạo mặc định không phải SuperAdmin (vì ID sẽ lớn hơn)
        
        if (userService.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
        if (userService.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }
        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setProvider("LOCAL");
        user.setActive(true);
        userRepository.save(user);
        
        activityService.log("user", "Admin " + actor.getUsername() + " đã tạo tài khoản mới: " + user.getUsername());
        return ResponseEntity.ok(user);
    }

    @PostMapping("/{id}/update-role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id, @RequestParam Long roleId) {
        User actor = getCurrentUser();
        User target = userRepository.findById(id).orElse(null);
        
        if (target == null) return ResponseEntity.notFound().build();
        
        if (!canManage(actor, target)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                .body("Bạn không có quyền thay đổi vai trò của quản trị viên này!");
        }

        try {
            userService.updateUserRole(id, roleId);
            String roleName = roleRepository.findById(roleId).map(Role::getName).orElse("Unknown");
            activityService.log("user", "Admin " + actor.getUsername() + " đã cập nhật vai trò cho " + target.getUsername() + " thành " + roleName);
            return ResponseEntity.ok("User role updated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update role: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id) {
        User actor = getCurrentUser();
        User target = userRepository.findById(id).orElse(null);
        
        if (target == null) return ResponseEntity.notFound().build();

        if (!canManage(actor, target)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                .body("Bạn không có quyền khóa/mở khóa tài khoản của quản trị viên này!");
        }

        target.setActive(!target.isEnabled());
        userRepository.save(target);
        String action = target.isEnabled() ? "đã mở khóa" : "đã khóa";
        activityService.log("user", "Admin " + actor.getUsername() + " " + action + " tài khoản: " + target.getUsername());
        return ResponseEntity.ok(target.isEnabled() ? "User activated" : "User locked");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        User actor = getCurrentUser();
        User target = userRepository.findById(id).orElse(null);
        
        if (target == null) return ResponseEntity.notFound().build();

        if (!canManage(actor, target)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                .body("Bạn không có quyền xóa tài khoản của quản trị viên này!");
        }

        activityService.log("user", "Admin " + actor.getUsername() + " đã xóa vĩnh viễn tài khoản: " + target.getUsername() + " (ID: " + id + ")");
        userRepository.delete(target);
        return ResponseEntity.ok("User deleted successfully");
    }
}
