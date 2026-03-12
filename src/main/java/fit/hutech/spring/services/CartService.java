package fit.hutech.spring.services;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import fit.hutech.spring.daos.Cart;
import fit.hutech.spring.daos.Item;
import fit.hutech.spring.entities.ShoppingCart;
import fit.hutech.spring.entities.ShoppingCartItem;
import fit.hutech.spring.entities.User;
import fit.hutech.spring.repositories.IBookRepository;
import fit.hutech.spring.repositories.IShoppingCartRepository;
import fit.hutech.spring.repositories.IUserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = { Exception.class, Throwable.class })
public class CartService {

    private static final String CART_SESSION_KEY = "cart";

    private final IBookRepository bookRepository;
    private final IUserRepository userRepository;
    private final IShoppingCartRepository shoppingCartRepository;
    private final fit.hutech.spring.repositories.IOrderRepository orderRepository;
    private final fit.hutech.spring.repositories.IOrderDetailRepository orderDetailRepository;

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                !authentication.getPrincipal().equals("anonymousUser")) {
            return userRepository.findByUsername(authentication.getName()).orElse(null);
        }
        return null;
    }

    public Cart getCart(@NotNull HttpSession session) {
        User user = getAuthenticatedUser();

        // 1. If User Logged In -> Load from DB
        if (user != null) {
            ShoppingCart entity = shoppingCartRepository.findByUserId(user.getId()).orElse(new ShoppingCart());
            if (entity.getId() == null) {
                entity.setUser(user);
                entity.setItems(new ArrayList<>());
                // Don't save yet, wait for update
            }

            // Map Entity -> DTO
            Cart cartDTO = new Cart();
            // Need to retrieve existing session cart to merge?
            // For now, let's keep it simple: DB overrides Session.
            // If we want merge, we should do it at login time (Handler).

            if (entity.getItems() != null) {
                for (ShoppingCartItem itemEntity : entity.getItems()) {
                    cartDTO.addItems(new Item(
                            itemEntity.getBook().getId(),
                            itemEntity.getBook().getTitle(),
                            itemEntity.getBook().getPrice(),
                            itemEntity.getQuantity(),
                            itemEntity.getBook().getImagePath()));
                }
            }
            return cartDTO;
        }

        // 2. If Anonymous -> Session
        return Optional.ofNullable((Cart) session.getAttribute(CART_SESSION_KEY))
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    session.setAttribute(CART_SESSION_KEY, cart);
                    return cart;
                });
    }

    public void updateCart(@NotNull HttpSession session, Cart cartDTO) {
        User user = getAuthenticatedUser();

        // 1. If User Logged In -> Save to DB
        if (user != null) {
            ShoppingCart entity = shoppingCartRepository.findByUserId(user.getId()).orElse(new ShoppingCart());
            if (entity.getId() == null) {
                entity.setUser(user);
                entity = shoppingCartRepository.save(entity); // Save first to get ID
            }

            // Clear old items and add new items
            // Note: Orphan Removal is enabled.
            // But replacing the list reference might not work with Hibernate managed
            // collection sometimes.
            // Better to clear and add.
            if (entity.getItems() == null)
                entity.setItems(new ArrayList<>());
            entity.getItems().clear();

            for (Item itemDTO : cartDTO.getCartItems()) {
                ShoppingCartItem itemEntity = new ShoppingCartItem();
                itemEntity.setCart(entity);
                itemEntity.setBook(bookRepository.findById(itemDTO.getBookId()).orElseThrow());
                itemEntity.setQuantity(itemDTO.getQuantity());
                entity.getItems().add(itemEntity);
            }

            shoppingCartRepository.save(entity);
        }

        // 2. Always update session (for immediate consistency during this request, or
        // fallback)
        session.setAttribute(CART_SESSION_KEY, cartDTO);
    }

    public void removeCart(@NotNull HttpSession session) {
        User user = getAuthenticatedUser();
        if (user != null) {
            shoppingCartRepository.findByUserId(user.getId()).ifPresent(cart -> {
                cart.getItems().clear();
                shoppingCartRepository.save(cart);
            });
        }
        session.removeAttribute(CART_SESSION_KEY);
    }

    public int getSumQuantity(@NotNull HttpSession session) {
        return getCart(session).getCartItems().stream()
                .mapToInt(Item::getQuantity)
                .sum();
    }

    public double getSumPrice(@NotNull HttpSession session) {
        return getCart(session).getCartItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
    }

    public void saveOrder(@NotNull HttpSession session, String receiverName, String phoneNumber, String address,
            String note, String paymentMethod) {
        var cart = getCart(session);
        if (cart.getCartItems().isEmpty())
            return;

        // 1. Tạo và lưu hóa đơn (Order)
        var order = new fit.hutech.spring.entities.Order();
        order.setOrderDate(java.time.LocalDateTime.now());
        order.setTotalPrice(getSumPrice(session));
        order.setShippingAddress(address);
        order.setReceiverName(receiverName);
        order.setPhoneNumber(phoneNumber);
        order.setNote(note);
        order.setPaymentMethod(paymentMethod);
        order.setStatus("PENDING");

        // Gán User hiện tại cho Order
        User user = getAuthenticatedUser();
        if (user != null)
            order.setUser(user);

        // Lưu Order trước để có ID
        orderRepository.save(order);

        // 2. Lưu chi tiết hóa đơn (OrderDetail)
        cart.getCartItems().forEach(item -> {
            var orderDetail = new fit.hutech.spring.entities.OrderDetail();
            orderDetail.setOrder(order);
            orderDetail.setQuantity(item.getQuantity());
            orderDetail.setPrice(item.getPrice());

            // Tìm sách theo ID và cập nhật lượt bán
            fit.hutech.spring.entities.Book book = bookRepository.findById(item.getBookId()).orElseThrow();
            orderDetail.setBook(book);
            
            int soldCount = (book.getTotalSold() != null ? book.getTotalSold() : 0) + item.getQuantity();
            book.setTotalSold(soldCount);
            bookRepository.save(book);

            orderDetailRepository.save(orderDetail);
        });

        // 3. Xóa giỏ hàng sau khi đã thanh toán thành công
        removeCart(session);
    }
}