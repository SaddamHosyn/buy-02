package ax.gritlab.buy_01.order.controller;

import ax.gritlab.buy_01.order.model.Cart;
import ax.gritlab.buy_01.order.model.CartItem;
import ax.gritlab.buy_01.order.repository.CartRepository;
import ax.gritlab.buy_01.order.security.AuthenticatedUser;
import ax.gritlab.buy_01.order.security.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Cart Controller with server-side authorization.
 * All cart operations use the authenticated user's ID.
 */
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class CartController {

   private final CartRepository cartRepository;
   private final AuthorizationService authService;

   /**
    * Get cart for the authenticated user.
    */
   @GetMapping
   public ResponseEntity<Cart> getCart() {
      AuthenticatedUser currentUser = authService.requireAuthentication();
      Optional<Cart> cart = cartRepository.findByUserId(currentUser.getUserId());
      return cart.map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.ok(createEmptyCart(currentUser.getUserId())));
   }

   /**
    * Add item to the authenticated user's cart.
    */
   @PostMapping("/items")
   public ResponseEntity<Cart> addItem(@RequestBody CartItem item) {
      AuthenticatedUser currentUser = authService.requireAuthentication();
      Cart cart = cartRepository.findByUserId(currentUser.getUserId())
            .orElseGet(() -> createEmptyCart(currentUser.getUserId()));
      item.setAddedAt(LocalDateTime.now());
      item.setUpdatedAt(LocalDateTime.now());
      cart.addItem(item);
      cart = cartRepository.save(cart);
      return ResponseEntity.ok(cart);
   }

   /**
    * Update item quantity in the authenticated user's cart.
    */
   @PutMapping("/items/{productId}")
   public ResponseEntity<Cart> updateItemQuantity(
         @PathVariable String productId,
         @RequestBody UpdateQuantityRequest request) {
      AuthenticatedUser currentUser = authService.requireAuthentication();
      Optional<Cart> cartOpt = cartRepository.findByUserId(currentUser.getUserId());
      if (cartOpt.isEmpty()) {
         return ResponseEntity.notFound().build();
      }

      Cart cart = cartOpt.get();
      cart.updateItemQuantity(productId, request.getQuantity());
      cart = cartRepository.save(cart);
      return ResponseEntity.ok(cart);
   }

   @lombok.Data
   public static class UpdateQuantityRequest {
      private int quantity;
   }

   /**
    * Remove item from the authenticated user's cart.
    */
   @DeleteMapping("/items/{productId}")
   public ResponseEntity<Cart> removeItem(@PathVariable String productId) {
      AuthenticatedUser currentUser = authService.requireAuthentication();
      Optional<Cart> cartOpt = cartRepository.findByUserId(currentUser.getUserId());
      if (cartOpt.isEmpty()) {
         return ResponseEntity.notFound().build();
      }

      Cart cart = cartOpt.get();
      cart.removeItem(productId);
      cart = cartRepository.save(cart);
      return ResponseEntity.ok(cart);
   }

   /**
    * Clear the authenticated user's cart.
    */
   @DeleteMapping
   public ResponseEntity<Void> clearCart() {
      AuthenticatedUser currentUser = authService.requireAuthentication();
      cartRepository.findByUserId(currentUser.getUserId()).ifPresent(cartRepository::delete);
      return ResponseEntity.noContent().build();
   }

   private Cart createEmptyCart(String userId) {
      Cart cart = new Cart();
      cart.setUserId(userId);
      return cart;
   }
}
