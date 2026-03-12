package fit.hutech.spring.repositories;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Bổ sung import này
import org.springframework.stereotype.Repository;

import fit.hutech.spring.entities.Book;

@Repository
public interface IBookRepository extends JpaRepository<Book, Long> {

    // Hàm mặc định có sẵn của bạn
    default List<Book> findAllBooks(Integer pageNo, Integer pageSize, String sortBy) {
        return findAll(PageRequest.of(pageNo, pageSize, Sort.by(sortBy)))
                .getContent();
    }

    // === BỔ SUNG: Phương thức tìm kiếm từ ảnh image_e6cd3c.png ===
    @Query("""
            SELECT b FROM Book b
            WHERE b.title LIKE %?1%
            OR b.author LIKE %?1%
            OR b.category.name LIKE %?1%
            """)
    List<Book> searchBook(String keyword);
    // =============================================================

    // TOP 4 Sách Nổi Bật (Có thể Staff bật tắt bằng isFeatured)
    List<Book> findTop4ByIsFeaturedTrueOrderByTotalSoldDesc();

    // TOP 4 Sách Đang Giảm Giá
    List<Book> findTop4ByIsOnSaleTrueOrderByDiscountPriceAsc();

    // TOP 20 Sách Mới Nhất (Sắp xếp theo ID giảm dần - ID lớn nhất là sách vừa thêm mới nhất)
    List<Book> findTop20ByOrderByIdDesc();

    // TOP 4 Sách Xem Nhiều Nhất
    List<Book> findTop4ByOrderByViewCountDesc();

    // === BỔ SUNG CHO DI CƯ DỮ LIỆU ===
    List<Book> findByCategoryId(Long categoryId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Book b SET b.category = ?1 WHERE b.category.id = ?2")
    void updateCategoryForBooks(fit.hutech.spring.entities.Category newCategory, Long oldCategoryId);
}