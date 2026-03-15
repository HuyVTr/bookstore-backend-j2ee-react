package fit.hutech.spring.controllers.rest;

import fit.hutech.spring.entities.SystemConfig;
import fit.hutech.spring.services.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/configs")
@RequiredArgsConstructor
public class SystemConfigRestController {

    private final SystemConfigService configService;

    @GetMapping("/public")
    public ResponseEntity<SystemConfig> getPublicSettings() {
        return ResponseEntity.ok(configService.getSystemConfig());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<SystemConfig> getSettings() {
        return ResponseEntity.ok(configService.getSystemConfig());
    }

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<SystemConfig> updateSettings(@RequestBody SystemConfig config) {
        return ResponseEntity.ok(configService.updateSettings(config));
    }

    @PostMapping("/clear-cache")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> clearCache() {
        configService.clearCache();
        return ResponseEntity.ok("Cache hệ thống đã được làm mới thành công.");
    }
}
