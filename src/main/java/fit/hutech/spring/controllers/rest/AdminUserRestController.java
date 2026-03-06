package fit.hutech.spring.controllers.rest;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    private final IUserRepository userRepository; // needed for saving isActive

    @GetMapping
    public ResponseEntity<List<User>> listUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/roles")
    public ResponseEntity<List<Role>> listAllRoles() {
        return ResponseEntity.ok(roleRepository.findAll());
    }

    @PostMapping("/{id}/update-role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id, @RequestParam Long roleId) {
        try {
            userService.updateUserRole(id, roleId);
            return ResponseEntity.ok("User role updated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update role: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id) {
        return userRepository.findById(id).map(user -> {
            user.setActive(!user.isEnabled());
            userRepository.save(user);
            return ResponseEntity.ok(user.isEnabled() ? "User activated" : "User locked");
        }).orElse(ResponseEntity.notFound().build());
    }
}
