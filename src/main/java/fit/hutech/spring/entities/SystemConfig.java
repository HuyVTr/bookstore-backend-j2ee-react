package fit.hutech.spring.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "system_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "website_name", nullable = false)
    private String websiteName;

    @Column(name = "order_prefix")
    private String orderPrefix;

    @Column(name = "currency")
    private String currency;

    @Column(name = "default_language")
    private String defaultLanguage;

    @Column(name = "maintenance_mode")
    private boolean maintenanceMode;

    @Column(name = "allow_registration")
    private boolean allowRegistration;
}
