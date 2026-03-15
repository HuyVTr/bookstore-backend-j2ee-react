package fit.hutech.spring.controllers.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fit.hutech.spring.dtos.LoginRequest;
import fit.hutech.spring.security.JwtUtils;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final fit.hutech.spring.services.UserService userService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final fit.hutech.spring.services.SystemConfigService configService;

    public AuthRestController(AuthenticationManager authenticationManager, JwtUtils jwtUtils,
            fit.hutech.spring.services.UserService userService,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
            fit.hutech.spring.services.SystemConfigService configService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.configService = configService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        // Kiểm tra chế độ bảo trì
        if (configService.getSystemConfig().isMaintenanceMode()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Hệ thống đang trong chế độ bảo trì. Vui lòng quay lại sau.");
        }
        
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("token", jwt);
        response.put("username", userDetails.getUsername());
        response.put("roles", roles);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@jakarta.validation.Valid @RequestBody fit.hutech.spring.entities.User user,
            org.springframework.validation.BindingResult bindingResult) {
        // Kiểm tra cấu hình cho phép đăng ký
        if (!configService.getSystemConfig().isAllowRegistration()) {
            return ResponseEntity.badRequest().body("Tính năng đăng ký hiện đang bị khóa bởi quản trị viên.");
        }

        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getAllErrors().stream()
                    .map(org.springframework.context.support.DefaultMessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(errors);
        }

        // Kiểm tra trùng lặp
        if (userService.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Error: Username is already taken!");
        }
        if (userService.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Error: Email is already taken!");
        }

        // Mã hóa mật khẩu và lưu
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setProvider("LOCAL");
        user.setActive(true);

        userService.save(user);
        userService.setDefaultRole(user.getUsername());

        return ResponseEntity.ok("User registered successfully!");
    }
}
