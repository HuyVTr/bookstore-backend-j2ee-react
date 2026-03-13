package fit.hutech.spring.services;

import fit.hutech.spring.entities.Review;
import fit.hutech.spring.repositories.ReviewRepository;
import fit.hutech.spring.repositories.IOrderDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final IOrderDetailRepository orderDetailRepository;

    public List<Review> getReviewsByBookId(Long bookId) {
        return reviewRepository.findByBookIdOrderByCreatedAtDesc(bookId);
    }

    @Transactional
    public Review addReview(Review review) {
        if (!orderDetailRepository.hasPurchasedBook(review.getUser().getId(), review.getBook().getId())) {
            throw new RuntimeException("Bạn chỉ có thể đánh giá những cuốn sách đã mua thành công.");
        }
        
        if (reviewRepository.existsByUserIdAndBookId(review.getUser().getId(), review.getBook().getId())) {
            throw new RuntimeException("Bạn đã đánh giá cuốn sách này rồi.");
        }

        return reviewRepository.save(review);
    }

    public boolean canUserReview(Long userId, Long bookId) {
        return orderDetailRepository.hasPurchasedBook(userId, bookId) && 
               !reviewRepository.existsByUserIdAndBookId(userId, bookId);
    }
}
