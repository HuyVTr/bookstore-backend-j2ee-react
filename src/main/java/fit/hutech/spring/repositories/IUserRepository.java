package fit.hutech.spring.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fit.hutech.spring.entities.User;

@Repository
public interface IUserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameOrEmail(String username, String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    @org.springframework.data.jpa.repository.Query("SELECT new fit.hutech.spring.dtos.PlatformStatsDTO(u.provider, COUNT(u)) "
            +
            "FROM User u GROUP BY u.provider")
    java.util.List<fit.hutech.spring.dtos.PlatformStatsDTO> countUsersByPlatform();
}