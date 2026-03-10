package fit.hutech.spring.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import fit.hutech.spring.entities.Book;
import fit.hutech.spring.repositories.IBookRepository;
import fit.hutech.spring.repositories.IOrderDetailRepository;
import org.springframework.data.domain.PageRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = { Exception.class, Throwable.class })
public class BookService {
    private final IBookRepository bookRepository;
    private final IOrderDetailRepository orderDetailRepository;
    private final fit.hutech.spring.repositories.IAuthorRepository authorRepository;

    public Book getBestSellingBook() {
        var topSelling = orderDetailRepository.findTopSellingBooks(PageRequest.of(0, 1));
        if (topSelling != null && !topSelling.isEmpty()) {
            return topSelling.get(0).getBook();
        }
        // Fallback: Lấy quyển sách mới nhất nếu chưa có đơn hàng nào
        return bookRepository.findAll(PageRequest.of(0, 1, org.springframework.data.domain.Sort.by("id").descending()))
                .getContent().stream().findFirst().orElse(null);
    }

    public java.util.List<fit.hutech.spring.dtos.CategorySalesDTO> getTopSellingCategories(int limit) {
        java.util.List<Object[]> results = orderDetailRepository.findTopSellingCategoriesRaw(PageRequest.of(0, limit));
        return results.stream().map(result -> {
            fit.hutech.spring.entities.Category category = (fit.hutech.spring.entities.Category) result[0];
            Long totalSold = (Long) result[1];
            return new fit.hutech.spring.dtos.CategorySalesDTO(category, totalSold);
        }).collect(java.util.stream.Collectors.toList());
    }

    public java.util.List<fit.hutech.spring.dtos.AuthorSalesDTO> getTopSellingAuthors(int limit) {
        var topAuthors = orderDetailRepository.findTopSellingAuthors(PageRequest.of(0, limit));

        if (topAuthors.isEmpty()) {
            // Fallback: Lấy các tác giả từ danh sách sách nếu chưa có lượt bán
            topAuthors = bookRepository.findAll().stream()
                    .map(b -> b.getAuthor())
                    .distinct()
                    .limit(limit)
                    .map(name -> new fit.hutech.spring.dtos.AuthorSalesDTO(name, 0L, ""))
                    .collect(java.util.stream.Collectors.toList());
        }

        // Ánh xạ Avatar từ Author metadata
        for (var authorDTO : topAuthors) {
            authorRepository.findByName(authorDTO.getAuthorName()).ifPresent(author -> {
                authorDTO.setAuthorImage(author.getAvatarPath());
            });
        }

        return topAuthors;
    }

    public List<Book> getAllBooks(Integer pageNo, Integer pageSize, String sortBy) {
        return bookRepository.findAllBooks(pageNo, pageSize, sortBy);
    }

    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }

    public void addBook(Book book) {
        bookRepository.save(book);
    }

    public void updateBook(@NotNull Book book) {
        Book existingBook = bookRepository.findById(book.getId())
                .orElse(null);

        if (existingBook != null) {
            existingBook.setTitle(book.getTitle());
            existingBook.setAuthor(book.getAuthor());
            existingBook.setPrice(book.getPrice());
            existingBook.setCategory(book.getCategory());

            // Chỉ cập nhật ảnh nếu có ảnh mới được cung cấp
            if (book.getImagePath() != null && !book.getImagePath().isEmpty()) {
                existingBook.setImagePath(book.getImagePath());
            }

            bookRepository.save(existingBook);
        }
    }

    public void deleteBookById(Long id) {
        bookRepository.deleteById(id);
    }

    public long countAllBooks() {
        return bookRepository.count();
    }

    // === BỔ SUNG: Phương thức tìm kiếm theo ảnh image_e6d09d.png ===
    public List<Book> searchBook(String keyword) {
        // Lọc lại kết quả bằng Java để phân biệt dấu tiếng Việt chính xác
        // (Vì MySQL mặc định collation utf8_general_ci thường bỏ qua dấu: a == á)
        String finalKeyword = keyword.toLowerCase();
        return bookRepository.searchBook(keyword).stream()
                .filter(book -> (book.getTitle() != null && book.getTitle().toLowerCase().contains(finalKeyword)) ||
                        (book.getAuthor() != null && book.getAuthor().toLowerCase().contains(finalKeyword)) ||
                        (book.getCategory() != null && book.getCategory().getName() != null
                                && book.getCategory().getName().toLowerCase().contains(finalKeyword)))
                .toList();
    }
    // ===============================================================

    // === Cập nhật số lượng sách (Nhập/Xuất kho) ===
    public void updateBookQuantity(Long bookId, Integer changeAmount) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + bookId));

        int newQuantity = (book.getQuantity() != null ? book.getQuantity() : 0) + changeAmount;

        if (newQuantity < 0) {
            throw new IllegalArgumentException("Số lượng tồn kho không đủ để xuất! Tồn tại: " + book.getQuantity());
        }

        book.setQuantity(newQuantity);
        bookRepository.save(book);
    }

    // === NEW: Lấy danh sách sách cho trang khách ===
    public List<Book> getFeaturedBooks() {
        List<Book> featured = bookRepository.findTop4ByIsFeaturedTrueOrderByTotalSoldDesc();
        if (featured.isEmpty()) {
            return bookRepository
                    .findAll(PageRequest.of(0, 4, org.springframework.data.domain.Sort.by("id").descending()))
                    .getContent();
        }
        return featured;
    }

    public List<Book> getOnSaleBooks() {
        List<Book> onSale = bookRepository.findTop4ByIsOnSaleTrueOrderByDiscountPriceAsc();
        if (onSale.isEmpty()) {
            return bookRepository
                    .findAll(PageRequest.of(0, 4, org.springframework.data.domain.Sort.by("price").ascending()))
                    .getContent();
        }
        return onSale;
    }

    public List<Book> getMostViewedBooks() {
        List<Book> mostViewed = bookRepository.findTop4ByOrderByViewCountDesc();
        if (mostViewed.isEmpty()) {
            return bookRepository.findAll(PageRequest.of(0, 4)).getContent();
        }
        return mostViewed;
    }

    // === DI CƯ DỮ LIỆU DANH MỤC (XỬ LÝ TRANSACTION) ===
    @Transactional
    public void migrateCategories(fit.hutech.spring.repositories.ICategoryRepository categoryRepository) {
        // 1. Merge IT sub-categories into "Lập Trình"
        String targetCatName = "Lập Trình";
        fit.hutech.spring.entities.Category programming = categoryRepository.findByName(targetCatName);
        if (programming == null) {
            programming = categoryRepository
                    .save(new fit.hutech.spring.entities.Category(null, targetCatName, null, null));
            programming.setIcon("💻");
            categoryRepository.save(programming);
        }

        String[] itCategories = { "Công nghệ phần mềm", "An toàn thông tin", "Hệ thống thông tin", "Mạng máy tính",
                "Khoa học dữ liệu" };
        for (String oldName : itCategories) {
            fit.hutech.spring.entities.Category oldCat = categoryRepository.findByName(oldName);
            if (oldCat != null) {
                // Reassign all books to the new category
                bookRepository.updateCategoryForBooks(programming, oldCat.getId());
                // Delete the now-empty old category
                categoryRepository.delete(oldCat);
                System.out.println("Migrated category: " + oldName + " -> " + targetCatName);
            }
        }

        // 2. Rename "Liên Minh Huyền Thoại" to "Game"
        fit.hutech.spring.entities.Category lolCat = categoryRepository.findByName("Liên Minh Huyền Thoại");
        if (lolCat != null) {
            lolCat.setName("Game");
            lolCat.setIcon("🎮");
            categoryRepository.save(lolCat);
            System.out.println("Renamed category: Liên Minh Huyền Thoại -> Game");
        }
    }
}