package ax.gritlab.buy_01.order.controller;

import ax.gritlab.buy_01.order.dto.request.AddToCartRequest;
import ax.gritlab.buy_01.order.dto.request.UpdateCartItemRequest;
import ax.gritlab.buy_01.order.dto.response.CartResponse;
import ax.gritlab.buy_01.order.model.User;
import ax.gritlab.buy_01.order.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * Get user's cart.
     */
    @GetMapping
    public ResponseEntity<CartResponse> getCart(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        CartResponse cart = cartService.getCart(user.getId());
        return ResponseEntity.ok(cart);
    }

    /**
     * Add item to cart.
     */
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @Valid @RequestBody AddToCartRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        CartResponse cart = cartService.addItem(user.getId(), request);
        return ResponseEntity.ok(cart);
    }

    /**
     * Update item quantity in cart.
     */
    @PatchMapping("/items/{productId}")
    public ResponseEntity<CartResponse> updateItem(
            @PathVariable String productId,
            @Valid @RequestBody UpdateCartItemRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        CartResponse cart = cartService.updateItem(user.getId(), productId, request.getQuantity());
        return ResponseEntity.ok(cart);
    }

    /**
     * Remove item from cart.
     */
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(
            @PathVariable String productId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        CartResponse cart = cartService.removeItem(user.getId(), productId);
        return ResponseEntity.ok(cart);
    }

    /**
     * Clear cart.
     */
    @DeleteMapping
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        cartService.clearCart(user.getId());
        return ResponseEntity.noContent().build();
    }
}
