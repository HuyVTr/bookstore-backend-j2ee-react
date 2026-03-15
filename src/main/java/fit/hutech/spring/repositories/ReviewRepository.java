package fit.hutech.spring.repositories;

import fit.hutech.spring.entities.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByBookIdOrderByCreatedAtDesc(Long bookId);
    boolean existsByUserIdAndBookId(Long userId, Long bookId);

    @org.springframework.data.jpa.repository.Query("SELECT AVG(r.rating) FROM Review r WHERE r.book.id = :bookId")
    Double findAverageRatingByBookId(Long bookId);

    long countByBookId(Long bookId);

    @org.springframework.data.jpa.repository.Query("SELECT AVG(r.rating) FROM Review r WHERE r.book.createdBy.id = :authorId")
    Double findAverageRatingByAuthorId(@org.springframework.data.repository.query.Param("authorId") Long authorId);
}
