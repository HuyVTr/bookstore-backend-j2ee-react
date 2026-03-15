package fit.hutech.spring.dtos;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class BookFormDTO {
    private String title;
    private String author;
    private Double price;
    private Long categoryId;
    private Integer quantity;
    private String description;
    private String publisher;
    private Integer publicationYear;
    private String dimensions;
    private String coverType;
    private Integer numberOfPages;
    private String language;
    private MultipartFile image;
    private MultipartFile[] subImages;
}
