package fit.hutech.spring.controllers.rest;

import fit.hutech.spring.entities.Review;
import fit.hutech.spring.entities.User;
import fit.hutech.spring.entities.Book;
import fit.hutech.spring.services.ReviewService;
import fit.hutech.spring.services.UserService;
import fit.hutech.spring.services.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ReviewRestController {

    private final ReviewService reviewService;
    private final UserService userService;
    private final BookService bookService;

    @GetMapping("/api/public/reviews/book/{bookId}")
    public ResponseEntity<?> getReviewsByBookId(@PathVariable Long bookId) {
        return ResponseEntity.ok(reviewService.getReviewsByBookId(bookId));
    }

    @GetMapping("/api/user/reviews/can-review/{bookId}")
    public ResponseEntity<?> canUserReview(@PathVariable Long bookId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return ResponseEntity.ok(false);
        return ResponseEntity.ok(reviewService.canUserReview(currentUser.getId(), bookId));
    }

    @PostMapping("/api/user/reviews")
    public ResponseEntity<?> addReview(@RequestBody Map<String, Object> payload) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return ResponseEntity.status(401).body("Unauthorized");

        Long bookId = Long.valueOf(payload.get("bookId").toString());
        Integer rating = (Integer) payload.get("rating");
        String comment = (String) payload.get("comment");

        Book book = bookService.getBookById(bookId).orElse(null);
        if (book == null) return ResponseEntity.status(404).body("Book not found");

        Review review = Review.builder()
                .book(book)
                .user(currentUser)
                .rating(rating)
                .comment(comment)
                .build();

        try {
            return ResponseEntity.ok(reviewService.addReview(review));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            return userService.findByUsername(userDetails.getUsername()).orElse(null);
        }
        return null;
    }
}
