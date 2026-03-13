package fit.hutech.spring.repositories;

import fit.hutech.spring.entities.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByBookIdOrderByCreatedAtDesc(Long bookId);
    boolean existsByUserIdAndBookId(Long userId, Long bookId);
}
