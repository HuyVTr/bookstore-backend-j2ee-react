package fit.hutech.spring.repositories;

import fit.hutech.spring.entities.SystemActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ISystemActivityRepository extends JpaRepository<SystemActivity, Long> {
    List<SystemActivity> findTop10ByOrderByCreatedAtDesc();
}
