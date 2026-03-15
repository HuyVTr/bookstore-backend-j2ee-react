package fit.hutech.spring.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fit.hutech.spring.entities.Order;
import fit.hutech.spring.repositories.IOrderRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final IOrderRepository orderRepository;
    private final UserService userService;

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public void saveOrder(Order order) {
        orderRepository.save(order);
    }

    public void updateOrderStatus(Long orderId, String status) {
        orderRepository.findById(orderId).ifPresent(order -> {
            String currentStatus = order.getStatus().toUpperCase();
            String newStatus = status.toUpperCase();

            // Trọng số trạng thái
            int currentWeight = getStatusWeight(currentStatus);
            int newWeight = getStatusWeight(newStatus);

            // Logic: Không cho phép quay ngược trạng thái (ngoại trừ CANCELLED có thể từ PENDING/SHIPPING)
            // Và không được sửa đơn đã COMPLETED hoặc CANCELLED
            if (currentStatus.equals("COMPLETED") || currentStatus.equals("CANCELLED")) {
                throw new IllegalStateException("Đơn hàng đã kết thúc, không thể thay đổi trạng thái.");
            }

            if (newWeight <= currentWeight && !newStatus.equals("CANCELLED")) {
                throw new IllegalStateException("Không thể chuyển ngược trạng thái đơn hàng.");
            }

            order.setStatus(newStatus);
            userService.getCurrentUser().ifPresent(order::setProcessedBy);
            orderRepository.save(order);
        });
    }

    private int getStatusWeight(String status) {
        switch (status) {
            case "PENDING": return 0;
            case "SHIPPING": return 1;
            case "COMPLETED": return 2;
            case "CANCELLED": return 3;
            default: return -1;
        }
    }
}
