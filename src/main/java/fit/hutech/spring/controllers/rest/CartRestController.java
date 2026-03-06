package fit.hutech.spring.controllers.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fit.hutech.spring.daos.Item;
import fit.hutech.spring.dtos.OrderRequest;
import fit.hutech.spring.services.BookService;
import fit.hutech.spring.services.CartService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartRestController {

    private final CartService cartService;
    private final BookService bookService;

    @GetMapping
    public ResponseEntity<?> getCart(HttpSession session) {
        return ResponseEntity.ok(cartService.getCart(session));
    }

    @PostMapping("/add/{id}")
    public ResponseEntity<?> addToCart(HttpSession session, @PathVariable Long id) {
        var book = bookService.getBookById(id);
        if (book.isPresent()) {
            var cart = cartService.getCart(session);
            cart.addItems(new Item(book.get().getId(), book.get().getTitle(), book.get().getPrice(), 1,
                    book.get().getImagePath()));
            cartService.updateCart(session, cart);
            return ResponseEntity.ok(cart);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/remove/{id}")
    public ResponseEntity<?> removeFromCart(HttpSession session, @PathVariable Long id) {
        var cart = cartService.getCart(session);
        cart.removeItems(id);
        cartService.updateCart(session, cart);
        return ResponseEntity.ok(cart);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateCart(HttpSession session, @PathVariable Long id, @RequestParam int quantity) {
        var cart = cartService.getCart(session);
        cart.updateItems(id, quantity);
        cartService.updateCart(session, cart);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clearCart(HttpSession session) {
        cartService.removeCart(session);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(HttpSession session, @RequestBody OrderRequest request) {
        cartService.saveOrder(session, request.getReceiverName(), request.getPhoneNumber(), request.getAddress(),
                request.getNote(), request.getPaymentMethod());
        return ResponseEntity.ok("Order placed successfully");
    }
}
