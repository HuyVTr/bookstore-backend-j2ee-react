package fit.hutech.spring.controllers.rest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import fit.hutech.spring.dtos.AuthorSalesDTO;
import fit.hutech.spring.dtos.BookFormDTO;
import fit.hutech.spring.dtos.CategorySalesDTO;
import fit.hutech.spring.entities.Book;
import fit.hutech.spring.entities.BookImage;
import fit.hutech.spring.entities.Category;
import fit.hutech.spring.services.BookService;
import lombok.RequiredArgsConstructor;

import fit.hutech.spring.repositories.ReviewRepository;

@RestController
@RequiredArgsConstructor
public class BookRestController {

    private final fit.hutech.spring.repositories.ICategoryRepository categoryRepository;
    private final BookService bookService;
    private final ReviewRepository reviewRepository;
    private final fit.hutech.spring.services.SystemActivityService activityService;

    @GetMapping("/api/public/categories")
    public ResponseEntity<?> getAllCategories() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    @GetMapping("/api/public/authors/top-selling")
    public ResponseEntity<java.util.List<AuthorSalesDTO>> getTopSellingAuthors(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(bookService.getTopSellingAuthors(limit));
    }

    private Map<String, Object> enrichOneBook(Book b) {
        if (b == null) return null;
        Map<String, Object> bMap = new HashMap<>();
        bMap.put("id", b.getId());
        bMap.put("title", b.getTitle());
        bMap.put("author", b.getAuthor());
        bMap.put("price", b.getPrice());
        bMap.put("discountPrice", b.getDiscountPrice());
        bMap.put("imagePath", b.getImagePath());
        bMap.put("quantity", b.getQuantity());
        
        if (b.getCategory() != null) {
            Map<String, Object> catMap = new HashMap<>();
            catMap.put("id", b.getCategory().getId());
            catMap.put("name", b.getCategory().getName());
            bMap.put("category", catMap);
        } else {
            bMap.put("category", null);
        }

        bMap.put("isFeatured", b.getIsFeatured());
        bMap.put("isOnSale", b.getIsOnSale());
        bMap.put("totalSold", b.getTotalSold());
        bMap.put("bookSource", b.getBookSource());
        bMap.put("description", b.getDescription());
        bMap.put("publisher", b.getPublisher());
        bMap.put("publicationYear", b.getPublicationYear());
        bMap.put("dimensions", b.getDimensions());
        bMap.put("coverType", b.getCoverType());
        bMap.put("numberOfPages", b.getNumberOfPages());
        bMap.put("language", b.getLanguage());
        
        // Thêm subImages sạch (dưới dạng object để tương thích với Frontend)
        if (b.getSubImages() != null) {
            java.util.List<Map<String, String>> subImgs = b.getSubImages().stream()
                .map(si -> {
                    Map<String, String> m = new HashMap<>();
                    m.put("imagePath", si.getImagePath());
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());
            bMap.put("subImages", subImgs);
        }

        Double avg = reviewRepository.findAverageRatingByBookId(b.getId());
        long reviewCount = reviewRepository.countByBookId(b.getId());
        bMap.put("averageRating", avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
        bMap.put("reviewCount", reviewCount);
        return bMap;
    }

    private java.util.List<Map<String, Object>> enrichBooks(java.util.List<Book> books) {
        java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Book b : books) {
            result.add(enrichOneBook(b));
        }
        return result;
    }

    @GetMapping("/api/public/books/featured")
    public ResponseEntity<?> getFeaturedBooks() {
        return ResponseEntity.ok(enrichBooks(bookService.getFeaturedBooks()));
    }

    @GetMapping("/api/public/books/on-sale")
    public ResponseEntity<?> getOnSaleBooks() {
        return ResponseEntity.ok(enrichBooks(bookService.getOnSaleBooks()));
    }

    @GetMapping("/api/public/books/most-viewed")
    public ResponseEntity<?> getMostViewedBooks() {
        return ResponseEntity.ok(enrichBooks(bookService.getMostViewedBooks()));
    }

    @GetMapping("/api/public/books/newest")
    public ResponseEntity<?> getNewestBooks() {
        return ResponseEntity.ok(enrichBooks(bookService.getNewestBooks()));
    }

    @GetMapping("/api/public/books/best-seller")
    public ResponseEntity<?> getBestSeller() {
        Book bestSeller = bookService.getBestSellingBook();
        if (bestSeller == null)
            return ResponseEntity.noContent().build();
        return ResponseEntity.ok(enrichOneBook(bestSeller));
    }

    @GetMapping("/api/public/categories/top-selling")
    public ResponseEntity<java.util.List<fit.hutech.spring.dtos.CategorySalesDTO>> getTopSellingCategories() {
        return ResponseEntity.ok(bookService.getTopSellingCategories(5));
    }

    @GetMapping("/api/public/books")
    public ResponseEntity<?> getAllBooks(
            @RequestParam(defaultValue = "0") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "false") Boolean saleOnly) {

        String sortField = "id";
        org.springframework.data.domain.Sort.Direction direction = org.springframework.data.domain.Sort.Direction.DESC;

        if ("price_asc".equals(sortBy)) {
            sortField = "price";
            direction = org.springframework.data.domain.Sort.Direction.ASC;
        } else if ("price_desc".equals(sortBy)) {
            sortField = "price";
            direction = org.springframework.data.domain.Sort.Direction.DESC;
        } else if ("popular".equals(sortBy)) {
            sortField = "totalSold";
            direction = org.springframework.data.domain.Sort.Direction.DESC;
        } else if ("newest".equals(sortBy)) {
            sortField = "id";
            direction = org.springframework.data.domain.Sort.Direction.DESC;
        }

        java.util.List<Book> allBooks = bookService.getAllBooks(0, Integer.MAX_VALUE, sortField);
        
        var filteredStream = allBooks.stream();
        if (category != null && !category.isBlank()) {
            filteredStream = filteredStream.filter(b -> b.getCategory() != null && b.getCategory().getName().equalsIgnoreCase(category));
        }
        if (minPrice != null) {
            filteredStream = filteredStream.filter(b -> b.getPrice() >= minPrice);
        }
        if (maxPrice != null) {
            filteredStream = filteredStream.filter(b -> b.getPrice() <= maxPrice);
        }
        if (search != null && !search.isBlank()) {
            String kw = search.toLowerCase();
            filteredStream = filteredStream.filter(b -> (b.getTitle() != null && b.getTitle().toLowerCase().contains(kw)) ||
                                                       (b.getAuthor() != null && b.getAuthor().toLowerCase().contains(kw)));
        }
        if (Boolean.TRUE.equals(saleOnly)) {
            filteredStream = filteredStream.filter(b -> b.getIsOnSale() != null && b.getIsOnSale());
        }

        java.util.List<Book> filteredBooks = filteredStream.collect(java.util.stream.Collectors.toList());
        int totalItems = filteredBooks.size();
        
        int start = pageNo * pageSize;
        int end = Math.min(start + pageSize, totalItems);
        java.util.List<Book> pagedBooks = (start < totalItems) ? filteredBooks.subList(start, end) : new java.util.ArrayList<>();

        // Enrich each book with rating data
        java.util.List<Map<String, Object>> enrichedBooks = new java.util.ArrayList<>();
        for (Book b : pagedBooks) {
            Map<String, Object> bMap = new HashMap<>();
            bMap.put("id", b.getId());
            bMap.put("title", b.getTitle());
            bMap.put("author", b.getAuthor());
            bMap.put("price", b.getPrice());
            bMap.put("discountPrice", b.getDiscountPrice());
            bMap.put("imagePath", b.getImagePath());
            bMap.put("quantity", b.getQuantity());
            bMap.put("category", b.getCategory());
            bMap.put("isFeatured", b.getIsFeatured());
            bMap.put("isOnSale", b.getIsOnSale());
            bMap.put("totalSold", b.getTotalSold());
            bMap.put("bookSource", b.getBookSource());
            bMap.put("description", b.getDescription());
            bMap.put("publisher", b.getPublisher());
            bMap.put("publicationYear", b.getPublicationYear());
            bMap.put("dimensions", b.getDimensions());
            bMap.put("coverType", b.getCoverType());
            bMap.put("numberOfPages", b.getNumberOfPages());
            bMap.put("language", b.getLanguage());
            Double avg = reviewRepository.findAverageRatingByBookId(b.getId());
            long reviewCount = reviewRepository.countByBookId(b.getId());
            bMap.put("averageRating", avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
            bMap.put("reviewCount", reviewCount);
            enrichedBooks.add(bMap);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("books", enrichedBooks);
        response.put("currentPage", pageNo);
        response.put("totalItems", (long)totalItems);
        response.put("totalPages", totalItems > 0 ? (int) Math.ceil((double) totalItems / pageSize) - 1 : 0);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/public/books/search")
    public ResponseEntity<?> searchBooks(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(defaultValue = "id") String sortBy) {

        var searchResults = bookService.searchBook(keyword);

        Map<String, Object> response = new HashMap<>();
        response.put("books", searchResults);
        response.put("currentPage", pageNo);
        response.put("totalPages",
                searchResults.size() > 0 ? (int) Math.ceil((double) searchResults.size() / pageSize) - 1 : 0);
        response.put("totalItems", searchResults.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/public/books/{id}")
    public ResponseEntity<?> getBookDetail(@PathVariable Long id) {
        return bookService.getBookById(id)
                .map(b -> ResponseEntity.ok(enrichOneBook(b)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/staff/books")
    public ResponseEntity<?> addBook(@ModelAttribute BookFormDTO form) {
        System.out.println("--- DEBUG ADD BOOK (ModelAttribute) ---");
        System.out.println("Title: " + form.getTitle());
        
        Book book = new Book();
        book.setTitle(form.getTitle());
        book.setAuthor(form.getAuthor());
        book.setPrice(form.getPrice());
        book.setQuantity(form.getQuantity() != null ? form.getQuantity() : 0);
        book.setDescription(form.getDescription());
        book.setPublisher(form.getPublisher());
        book.setPublicationYear(form.getPublicationYear());
        book.setDimensions(form.getDimensions());
        book.setCoverType(form.getCoverType());
        book.setNumberOfPages(form.getNumberOfPages());
        book.setLanguage(form.getLanguage());

        Category category = new Category();
        category.setId(form.getCategoryId());
        book.setCategory(category);

        if (form.getImage() != null && !form.getImage().isEmpty()) {
            try {
                String imageName = saveImageStatic(form.getImage());
                book.setImagePath("/images/books/" + imageName);
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not upload image");
            }
        }

        if (form.getSubImages() != null && form.getSubImages().length > 0) {
            for (MultipartFile file : form.getSubImages()) {
                if (file != null && !file.isEmpty()) {
                    try {
                        String name = saveImageStatic(file);
                        BookImage bi = BookImage.builder()
                                .imagePath("/images/books/" + name)
                                .book(book)
                                .build();
                        book.getSubImages().add(bi);
                    } catch (IOException e) {
                    }
                }
            }
        }

        bookService.addBook(book);
        activityService.log("book", "Đã thêm sách mới: " + book.getTitle());
        Map<String, Object> result = new HashMap<>();
        result.put("id", book.getId());
        result.put("title", book.getTitle());
        result.put("message", "Book added successfully");
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/api/staff/books/{id}")
    public ResponseEntity<?> updateBook(@PathVariable Long id, @ModelAttribute BookFormDTO form) {
        System.out.println("--- DEBUG UPDATE BOOK (ModelAttribute) ---");
        
        Book book = new Book();
        book.setId(id);
        book.setTitle(form.getTitle());
        book.setAuthor(form.getAuthor());
        book.setPrice(form.getPrice());
        book.setDescription(form.getDescription());
        book.setPublisher(form.getPublisher());
        book.setPublicationYear(form.getPublicationYear());
        book.setDimensions(form.getDimensions());
        book.setCoverType(form.getCoverType());
        book.setNumberOfPages(form.getNumberOfPages());
        book.setLanguage(form.getLanguage());
        if (form.getQuantity() != null) {
            book.setQuantity(form.getQuantity());
        }

        Category category = new Category();
        category.setId(form.getCategoryId());
        book.setCategory(category);

        if (form.getImage() != null && !form.getImage().isEmpty()) {
            try {
                String imageName = saveImageStatic(form.getImage());
                book.setImagePath("/images/books/" + imageName);
            } catch (IOException e) {
            }
        }

        if (form.getSubImages() != null && form.getSubImages().length > 0) {
            book.setSubImages(new ArrayList<>()); // Clear old if we want to replace
            for (MultipartFile file : form.getSubImages()) {
                if (file != null && !file.isEmpty()) {
                    try {
                        String name = saveImageStatic(file);
                        BookImage bi = BookImage.builder()
                                .imagePath("/images/books/" + name)
                                .book(book)
                                .build();
                        book.getSubImages().add(bi);
                    } catch (IOException e) {
                    }
                }
            }
        }

        bookService.updateBook(book);
        activityService.log("book", "Đã cập nhật thông tin sách: " + book.getTitle());
        Map<String, Object> result = new HashMap<>();
        result.put("id", book.getId());
        result.put("title", book.getTitle());
        result.put("message", "Book updated successfully");
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/api/staff/books/{id}/featured")
    public ResponseEntity<?> toggleFeatured(@PathVariable Long id) {
        return bookService.getBookById(id).map(book -> {
            book.setIsFeatured(!Boolean.TRUE.equals(book.getIsFeatured()));
            bookService.updateBook(book);
            activityService.log("book", "Đã " + (book.getIsFeatured() ? "đặt làm SP nổi bật: " : "hủy SP nổi bật: ") + book.getTitle());
            Map<String, Object> result = new HashMap<>();
            result.put("id", book.getId());
            result.put("isFeatured", book.getIsFeatured());
            return ResponseEntity.ok(result);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/staff/books/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable Long id) {
        try {
            return bookService.getBookById(id).map(book -> {
                bookService.deleteBookById(id);
                activityService.log("book", "Đã xóa sách: " + book.getTitle() + " (ID: " + id + ")");
                return ResponseEntity.ok("Đã xóa sách thành công");
            }).orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Không thể xóa sách. Có thể sách này đã có trong đơn hàng của khách.");
        }
    }

    @PatchMapping("/api/staff/books/{id}/quantity")
    public ResponseEntity<?> updateBookQuantity(@PathVariable Long id, @RequestParam Integer changeAmount) {
        try {
            bookService.updateBookQuantity(id, changeAmount);
            bookService.getBookById(id).ifPresent(b -> {
                activityService.log("stock", "Đã cập nhật kho cho sách: " + b.getTitle() + " (Thay đổi: " + (changeAmount > 0 ? "+" : "") + changeAmount + ")");
            });
            return ResponseEntity.ok("Quantity updated");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private String saveImageStatic(MultipartFile image) throws IOException {
        Path uploadPath = Paths.get("uploads", "books");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String filename = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
        Path filePath = uploadPath.resolve(filename);
        Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return filename;
    }
}
