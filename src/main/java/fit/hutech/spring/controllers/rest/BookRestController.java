package fit.hutech.spring.controllers.rest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import fit.hutech.spring.dtos.AuthorSalesDTO;
import fit.hutech.spring.dtos.CategorySalesDTO;
import fit.hutech.spring.entities.Book;
import fit.hutech.spring.entities.Category;
import fit.hutech.spring.services.BookService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BookRestController {

    private final BookService bookService;

    @GetMapping("/api/public/authors/top-selling")
    public ResponseEntity<java.util.List<AuthorSalesDTO>> getTopSellingAuthors(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(bookService.getTopSellingAuthors(limit));
    }

    @GetMapping("/api/public/books/featured")
    public ResponseEntity<java.util.List<Book>> getFeaturedBooks() {
        return ResponseEntity.ok(bookService.getFeaturedBooks());
    }

    @GetMapping("/api/public/books/on-sale")
    public ResponseEntity<java.util.List<Book>> getOnSaleBooks() {
        return ResponseEntity.ok(bookService.getOnSaleBooks());
    }

    @GetMapping("/api/public/books/most-viewed")
    public ResponseEntity<java.util.List<Book>> getMostViewedBooks() {
        return ResponseEntity.ok(bookService.getMostViewedBooks());
    }

    @GetMapping("/api/public/books/newest")
    public ResponseEntity<java.util.List<Book>> getNewestBooks() {
        return ResponseEntity.ok(bookService.getNewestBooks());
    }

    @GetMapping("/api/public/books/best-seller")
    public ResponseEntity<?> getBestSeller() {
        Book bestSeller = bookService.getBestSellingBook();
        if (bestSeller == null)
            return ResponseEntity.noContent().build();
        return ResponseEntity.ok(bestSeller);
    }

    @GetMapping("/api/public/categories/top-selling")
    public ResponseEntity<java.util.List<fit.hutech.spring.dtos.CategorySalesDTO>> getTopSellingCategories() {
        return ResponseEntity.ok(bookService.getTopSellingCategories(5));
    }

    @GetMapping("/api/public/books")
    public ResponseEntity<?> getAllBooks(
            @RequestParam(defaultValue = "0") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(defaultValue = "id") String sortBy) {

        Map<String, Object> response = new HashMap<>();
        response.put("books", bookService.getAllBooks(pageNo, pageSize, sortBy));
        response.put("currentPage", pageNo);

        long totalBooks = bookService.countAllBooks();
        response.put("totalPages", totalBooks > 0 ? (int) Math.ceil((double) totalBooks / pageSize) - 1 : 0);
        response.put("totalItems", totalBooks);

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
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==========================================
    // STAFF / ADMIN APIS
    // ==========================================

    @PostMapping("/api/staff/books")
    public ResponseEntity<?> addBook(
            @RequestParam("title") String title,
            @RequestParam("author") String author,
            @RequestParam("price") Double price,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(value = "quantity", defaultValue = "0") Integer quantity,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {

        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setPrice(price);
        book.setQuantity(quantity);

        Category category = new Category();
        category.setId(categoryId);
        book.setCategory(category);

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String imageName = saveImageStatic(imageFile);
                book.setImagePath("/images/books/" + imageName);
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not upload image");
            }
        }

        bookService.addBook(book);
        return ResponseEntity.status(HttpStatus.CREATED).body(book);
    }

    @PutMapping("/api/staff/books/{id}")
    public ResponseEntity<?> updateBook(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam("author") String author,
            @RequestParam("price") Double price,
            @RequestParam("categoryId") Long categoryId,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {

        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setAuthor(author);
        book.setPrice(price);

        Category category = new Category();
        category.setId(categoryId);
        book.setCategory(category);

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String imageName = saveImageStatic(imageFile);
                book.setImagePath("/images/books/" + imageName);
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not upload image");
            }
        }

        bookService.updateBook(book);
        return ResponseEntity.ok(book);
    }

    @DeleteMapping("/api/staff/books/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable Long id) {
        return bookService.getBookById(id).map(book -> {
            bookService.deleteBookById(id);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/api/staff/books/{id}/quantity")
    public ResponseEntity<?> updateBookQuantity(@PathVariable Long id, @RequestParam Integer changeAmount) {
        try {
            bookService.updateBookQuantity(id, changeAmount);
            return ResponseEntity.ok("Quantity updated");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Helper function for uploading image
    private String saveImageStatic(MultipartFile image) throws IOException {
        Path uploadPath = Paths.get("uploads");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String filename = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
        Path filePath = uploadPath.resolve(filename);
        Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return filename;
    }
}
