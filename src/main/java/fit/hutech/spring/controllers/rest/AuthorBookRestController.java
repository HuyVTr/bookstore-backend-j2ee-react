package fit.hutech.spring.controllers.rest;

import fit.hutech.spring.dtos.BookFormDTO;
import fit.hutech.spring.entities.Book;
import fit.hutech.spring.entities.BookImage;
import fit.hutech.spring.entities.Category;
import fit.hutech.spring.entities.User;
import fit.hutech.spring.repositories.IBookRepository;
import fit.hutech.spring.services.BookService;
import fit.hutech.spring.services.SystemActivityService;
import fit.hutech.spring.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@RestController
@RequestMapping("/api/author/books")
@RequiredArgsConstructor
public class AuthorBookRestController {

    private final IBookRepository bookRepository;
    private final BookService bookService;
    private final UserService userService;
    private final SystemActivityService activityService;

    @GetMapping
    public ResponseEntity<?> getMyBooks() {
        User currentUser = getCurrentUser();
        if (currentUser == null) return ResponseEntity.status(401).body("Unauthorized");

        List<Book> myBooks = bookRepository.findByCreatedById(currentUser.getId());
        return ResponseEntity.ok(myBooks);
    }

    @PostMapping
    public ResponseEntity<?> addBook(@ModelAttribute BookFormDTO form) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return ResponseEntity.status(401).body("Unauthorized");

        Book book = new Book();
        updateBookFromForm(book, form);
        book.setCreatedBy(currentUser);
        book.setBookSource("AUTHOR");
        
        // Handle images
        handleImages(book, form);

        bookService.addBook(book);
        activityService.log("book", "Tác giả " + currentUser.getFullName() + " đã xuất bản sách mới: " + book.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED).body(book);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateBook(@PathVariable Long id, @ModelAttribute BookFormDTO form) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return ResponseEntity.status(401).body("Unauthorized");

        Optional<Book> existing = bookRepository.findById(id);
        if (existing.isEmpty()) return ResponseEntity.notFound().build();
        
        Book book = existing.get();
        if (!book.getCreatedBy().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).body("You can only edit your own books");
        }

        updateBookFromForm(book, form);
        handleImages(book, form);

        bookService.updateBook(book);
        activityService.log("book", "Tác giả " + currentUser.getFullName() + " đã cập nhật sách: " + book.getTitle());
        return ResponseEntity.ok(book);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return ResponseEntity.status(401).body("Unauthorized");

        Optional<Book> existing = bookRepository.findById(id);
        if (existing.isEmpty()) return ResponseEntity.notFound().build();

        Book book = existing.get();
        if (!book.getCreatedBy().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).body("You can only delete your own books");
        }

        bookRepository.deleteById(id);
        activityService.log("book", "Tác giả " + currentUser.getFullName() + " đã gỡ bỏ sách: " + book.getTitle());
        return ResponseEntity.ok("Book removed successfully");
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            return null;
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userService.findByUsername(userDetails.getUsername()).orElse(null);
    }

    private void updateBookFromForm(Book book, BookFormDTO form) {
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
    }

    private void handleImages(Book book, BookFormDTO form) {
        if (form.getImage() != null && !form.getImage().isEmpty()) {
            try {
                String imageName = saveImage(form.getImage());
                book.setImagePath("/images/books/" + imageName);
            } catch (IOException e) {}
        }
        if (form.getSubImages() != null && form.getSubImages().length > 0) {
            if (book.getSubImages() == null) book.setSubImages(new ArrayList<>());
            else book.getSubImages().clear();
            
            for (MultipartFile file : form.getSubImages()) {
                if (file != null && !file.isEmpty()) {
                    try {
                        String name = saveImage(file);
                        BookImage bi = BookImage.builder()
                                .imagePath("/images/books/" + name)
                                .book(book)
                                .build();
                        book.getSubImages().add(bi);
                    } catch (IOException e) {}
                }
            }
        }
    }

    private String saveImage(MultipartFile image) throws IOException {
        Path uploadPath = Paths.get("uploads", "books");
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
        String filename = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
        Path filePath = uploadPath.resolve(filename);
        Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return filename;
    }
}
