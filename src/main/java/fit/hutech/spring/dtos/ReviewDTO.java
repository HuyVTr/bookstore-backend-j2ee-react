package fit.hutech.spring.dtos;

import fit.hutech.spring.entities.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDTO {
    private Long id;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String fullName;
    }

    public static ReviewDTO fromEntity(Review review) {
        UserInfo userInfo = null;
        if (review.getUser() != null) {
            userInfo = UserInfo.builder()
                    .id(review.getUser().getId())
                    .username(review.getUser().getUsername())
                    .fullName(review.getUser().getFullName())
                    .build();
        }

        return ReviewDTO.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .user(userInfo)
                .build();
    }
}
