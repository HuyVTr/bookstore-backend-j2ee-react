package fit.hutech.spring.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fit.hutech.spring.entities.Order;

@Repository
public interface IOrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT new fit.hutech.spring.dtos.UserSpendingDTO(o.user, SUM(o.totalPrice)) "
            +
            "FROM Order o GROUP BY o.user ORDER BY SUM(o.totalPrice) DESC")
    java.util.List<fit.hutech.spring.dtos.UserSpendingDTO> findTopSpenders(
            org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o WHERE o.status = 'COMPLETED'")
    Double getTotalRevenue();

    @org.springframework.data.jpa.repository.Query(value = "SELECT MONTH(o.order_date) as month, SUM(od.quantity) as quantity " +
            "FROM items_order o JOIN order_detail od ON o.id = od.order_id " +
            "WHERE o.status = 'COMPLETED' AND YEAR(o.order_date) = :year " +
            "GROUP BY MONTH(o.order_date) ORDER BY month ASC", nativeQuery = true)
    List<Object[]> getMonthlyBooksSoldByYear(@org.springframework.data.repository.query.Param("year") int year);

    @org.springframework.data.jpa.repository.Query(value = "SELECT MONTH(o.order_date) as month, SUM(o.total_price) as revenue " +
            "FROM items_order o " +
            "WHERE o.status = 'COMPLETED' AND YEAR(o.order_date) = :year " +
            "GROUP BY MONTH(o.order_date) ORDER BY month ASC", nativeQuery = true)
    List<Object[]> getMonthlyRevenueByYear(@org.springframework.data.repository.query.Param("year") int year);

    long countByProcessedById(Long userId);

    long countByStatus(String status);

    @org.springframework.data.jpa.repository.Query("SELECT o.paymentMethod, COUNT(o) FROM Order o WHERE o.status = 'COMPLETED' GROUP BY o.paymentMethod")
    List<Object[]> countByPaymentMethod();

    @org.springframework.data.jpa.repository.Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.status = 'COMPLETED' AND MONTH(o.orderDate) = :month AND YEAR(o.orderDate) = :year")
    Double getRevenueByMonthAndYear(@org.springframework.data.repository.query.Param("month") int month, @org.springframework.data.repository.query.Param("year") int year);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(o) FROM Order o WHERE YEAR(o.orderDate) = :year")
    long countByYear(@org.springframework.data.repository.query.Param("year") int year);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.status = 'COMPLETED' AND YEAR(o.orderDate) = :year")
    Double getTotalRevenueByYear(@org.springframework.data.repository.query.Param("year") int year);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(od.quantity) FROM OrderDetail od JOIN od.order o WHERE o.status = 'COMPLETED' AND od.book.createdBy.id = :authorId")
    Long getTotalSalesByAuthorId(@org.springframework.data.repository.query.Param("authorId") Long authorId);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(od.price * od.quantity) FROM OrderDetail od JOIN od.order o WHERE o.status = 'COMPLETED' AND od.book.createdBy.id = :authorId")
    Double getTotalRevenueByAuthorId(@org.springframework.data.repository.query.Param("authorId") Long authorId);

    @org.springframework.data.jpa.repository.Query(value = "SELECT MONTH(o.order_date) as month, SUM(od.quantity) as quantity " +
            "FROM items_order o JOIN order_detail od ON o.id = od.order_id " +
            "JOIN book b ON od.book_id = b.id " +
            "WHERE o.status = 'COMPLETED' AND b.created_by = :authorId AND YEAR(o.order_date) = :year " +
            "GROUP BY MONTH(o.order_date) ORDER BY month ASC", nativeQuery = true)
    List<Object[]> getAuthorMonthlySales(@org.springframework.data.repository.query.Param("authorId") Long authorId, @org.springframework.data.repository.query.Param("year") int year);

    @org.springframework.data.jpa.repository.Query("SELECT o.user.fullName, b.title, o.status, o.orderDate " +
            "FROM OrderDetail od JOIN od.order o JOIN od.book b " +
            "WHERE b.createdBy.id = :authorId " +
            "ORDER BY o.orderDate DESC")
    List<Object[]> getAuthorRecentActivities(@org.springframework.data.repository.query.Param("authorId") Long authorId, org.springframework.data.domain.Pageable pageable);
}
