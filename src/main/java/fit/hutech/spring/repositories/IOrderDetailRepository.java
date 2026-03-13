package fit.hutech.spring.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fit.hutech.spring.entities.OrderDetail;

@Repository
public interface IOrderDetailRepository extends JpaRepository<OrderDetail, Long> {

        @org.springframework.data.jpa.repository.Query("SELECT new fit.hutech.spring.dtos.BookSalesDTO(d.book, SUM(d.quantity)) "
                        +
                        "FROM OrderDetail d GROUP BY d.book ORDER BY SUM(d.quantity) DESC")
        java.util.List<fit.hutech.spring.dtos.BookSalesDTO> findTopSellingBooks(
                        org.springframework.data.domain.Pageable pageable);

        @org.springframework.data.jpa.repository.Query("SELECT b.category, SUM(d.quantity) "
                        +
                        "FROM OrderDetail d JOIN d.book b GROUP BY b.category ORDER BY SUM(d.quantity) DESC")
        java.util.List<Object[]> findTopSellingCategoriesRaw(org.springframework.data.domain.Pageable pageable);

        @org.springframework.data.jpa.repository.Query("SELECT new fit.hutech.spring.dtos.AuthorSalesDTO(b.author, SUM(d.quantity), '', 0L) "
                        +
                        "FROM OrderDetail d JOIN d.book b GROUP BY b.author ORDER BY SUM(d.quantity) DESC")
        java.util.List<fit.hutech.spring.dtos.AuthorSalesDTO> findTopSellingAuthors(
                        org.springframework.data.domain.Pageable pageable);

        @org.springframework.data.jpa.repository.Query("SELECT COUNT(d) > 0 FROM OrderDetail d WHERE d.order.user.id = :userId AND d.book.id = :bookId AND d.order.status = 'DELIVERED'")
        boolean hasPurchasedBook(@org.springframework.data.repository.query.Param("userId") Long userId, @org.springframework.data.repository.query.Param("bookId") Long bookId);
}
