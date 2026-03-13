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
import fit.hutech.spring.dtos.CategorySalesDTO;
import fit.hutech.spring.entities.Book;
import fit.hutech.spring.entities.BookImage;
import fit.hutech.spring.entities.Category;
import fit.hutech.spring.services.BookService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BookRestController {

    private final fit.hutech.spring.repositories.ICategoryRepository categoryRepository;
    private final BookService bookService;

    @GetMapping("/api/public/categories")
    public ResponseEntity<?> getAllCategories() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

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

        Map<String, Object> response = new HashMap<>();
        response.put("books", pagedBooks);
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
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/staff/books")
    public ResponseEntity<?> addBook(
            @RequestParam("title") String title,
            @RequestParam("author") String author,
            @RequestParam("price") Double price,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(value = "quantity", defaultValue = "0") Integer quantity,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "publisher", required = false) String publisher,
            @RequestParam(value = "publicationYear", required = false) Integer publicationYear,
            @RequestParam(value = "dimensions", required = false) String dimensions,
            @RequestParam(value = "coverType", required = false) String coverType,
            @RequestParam(value = "numberOfPages", required = false) Integer numberOfPages,
            @RequestParam(value = "language", required = false) String language,
            @RequestPart(value = "image", required = false) MultipartFile imageFile,
            @RequestPart(value = "subImages", required = false) MultipartFile[] subImages) {

        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setPrice(price);
        book.setQuantity(quantity);
        book.setDescription(description);
        book.setPublisher(publisher);
        book.setPublicationYear(publicationYear);
        book.setDimensions(dimensions);
        book.setCoverType(coverType);
        book.setNumberOfPages(numberOfPages);
        book.setLanguage(language);

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

        if (subImages != null && subImages.length > 0) {
            for (MultipartFile file : subImages) {
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
        return ResponseEntity.status(HttpStatus.CREATED).body(book);
    }

    @PutMapping("/api/staff/books/{id}")
    public ResponseEntity<?> updateBook(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam("author") String author,
            @RequestParam("price") Double price,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "publisher", required = false) String publisher,
            @RequestParam(value = "publicationYear", required = false) Integer publicationYear,
            @RequestParam(value = "dimensions", required = false) String dimensions,
            @RequestParam(value = "coverType", required = false) String coverType,
            @RequestParam(value = "numberOfPages", required = false) Integer numberOfPages,
            @RequestParam(value = "language", required = false) String language,
            @RequestPart(value = "image", required = false) MultipartFile imageFile,
            @RequestPart(value = "subImages", required = false) MultipartFile[] subImages) {

        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setAuthor(author);
        book.setPrice(price);
        book.setDescription(description);
        book.setPublisher(publisher);
        book.setPublicationYear(publicationYear);
        book.setDimensions(dimensions);
        book.setCoverType(coverType);
        book.setNumberOfPages(numberOfPages);
        book.setLanguage(language);

        Category category = new Category();
        category.setId(categoryId);
        book.setCategory(category);

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String imageName = saveImageStatic(imageFile);
                book.setImagePath("/images/books/" + imageName);
            } catch (IOException e) {
            }
        }

        if (subImages != null && subImages.length > 0) {
            book.setSubImages(new ArrayList<>()); // Clear old if we want to replace
            for (MultipartFile file : subImages) {
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
