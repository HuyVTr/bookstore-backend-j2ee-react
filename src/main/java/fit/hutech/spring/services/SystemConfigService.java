package fit.hutech.spring.services;

import fit.hutech.spring.entities.SystemConfig;
import fit.hutech.spring.repositories.ISystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final ISystemConfigRepository configRepository;
    private final SystemActivityService activityService;

    public SystemConfig getSystemConfig() {
        return configRepository.findFirstByOrderByIdAsc()
                .orElseGet(this::createDefaultConfig);
    }

    @Transactional
    public SystemConfig updateSettings(SystemConfig newConfig) {
        SystemConfig existing = configRepository.findFirstByOrderByIdAsc()
                .orElseGet(this::createDefaultConfig);
        
        existing.setWebsiteName(newConfig.getWebsiteName());
        existing.setOrderPrefix(newConfig.getOrderPrefix());
        existing.setCurrency(newConfig.getCurrency());
        existing.setDefaultLanguage(newConfig.getDefaultLanguage());
        existing.setMaintenanceMode(newConfig.isMaintenanceMode());
        existing.setAllowRegistration(newConfig.isAllowRegistration());
        
        SystemConfig saved = configRepository.save(existing);
        activityService.log("system", "Cập nhật cấu hình hệ thống: " + saved.getWebsiteName());
        return saved;
    }

    private SystemConfig createDefaultConfig() {
        SystemConfig config = SystemConfig.builder()
                .websiteName("Antigravity Bookstore")
                .orderPrefix("BK-")
                .currency("VND")
                .defaultLanguage("vi")
                .maintenanceMode(false)
                .allowRegistration(true)
                .build();
        return configRepository.save(config);
    }

    public void clearCache() {
        // Implement cache clearing logic if using Spring Cache
        // e.g., cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        System.out.println("System cache cleared by admin.");
        activityService.log("system", "Hành động: Làm mới bộ nhớ đệm (Clear Cache)");
    }
}
