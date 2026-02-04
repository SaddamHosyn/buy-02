package ax.gritlab.buy_01.order.controller;

import ax.gritlab.buy_01.order.model.Cart;
import ax.gritlab.buy_01.order.model.CartItem;
import ax.gritlab.buy_01.order.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class CartController {

   private final CartRepository cartRepository;

   @GetMapping("/{userId}")
   public ResponseEntity<Cart> getCart(@PathVariable String userId) {
      Optional<Cart> cart = cartRepository.findByUserId(userId);
      return cart.map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.ok(createEmptyCart(userId)));
   }

   @PostMapping("/{userId}/items")
   public ResponseEntity<Cart> addItem(@PathVariable String userId, @RequestBody CartItem item) {
      Cart cart = cartRepository.findByUserId(userId)
            .orElseGet(() -> createEmptyCart(userId));
      item.setAddedAt(LocalDateTime.now());
      item.setUpdatedAt(LocalDateTime.now());
      cart.addItem(item);
      cart = cartRepository.save(cart);
      return ResponseEntity.ok(cart);
   }

   @PutMapping("/{userId}/items/{productId}")
   public ResponseEntity<Cart> updateItemQuantity(
         @PathVariable String userId,
         @PathVariable String productId,
         @RequestParam int quantity) {
      Optional<Cart> cartOpt = cartRepository.findByUserId(userId);
      if (cartOpt.isEmpty()) {
         return ResponseEntity.notFound().build();
      }

      Cart cart = cartOpt.get();
      cart.updateItemQuantity(productId, quantity);
      cart = cartRepository.save(cart);
      return ResponseEntity.ok(cart);
   }

   @DeleteMapping("/{userId}/items/{productId}")
   public ResponseEntity<Cart> removeItem(
         @PathVariable String userId,
         @PathVariable String productId) {
      Optional<Cart> cartOpt = cartRepository.findByUserId(userId);
      if (cartOpt.isEmpty()) {
         return ResponseEntity.notFound().build();
      }

      Cart cart = cartOpt.get();
      cart.removeItem(productId);
      cart = cartRepository.save(cart);
      return ResponseEntity.ok(cart);
   }

   @DeleteMapping("/{userId}")
   public ResponseEntity<Void> clearCart(@PathVariable String userId) {
      cartRepository.findByUserId(userId).ifPresent(cartRepository::delete);
      return ResponseEntity.noContent().build();
   }

   private Cart createEmptyCart(String userId) {
      Cart cart = new Cart();
      cart.setUserId(userId);
      return cart;
   }
}
