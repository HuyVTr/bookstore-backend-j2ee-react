package fit.hutech.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BookstoreBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookstoreBackendApplication.class, args);
    }

    @org.springframework.context.annotation.Bean
    org.springframework.boot.CommandLineRunner init(fit.hutech.spring.repositories.IRoleRepository roleRepository) {
        return args -> {
            if (roleRepository.findByName("ADMIN") == null) {
                roleRepository.save(new fit.hutech.spring.entities.Role(null, "ADMIN", null, null));
            }
            if (roleRepository.findByName("USER") == null) {
                roleRepository.save(new fit.hutech.spring.entities.Role(null, "USER", null, null));
            }
            if (roleRepository.findByName("STAFF") == null) {
                roleRepository.save(new fit.hutech.spring.entities.Role(null, "STAFF", null, null));
            }
        };
    }
}
