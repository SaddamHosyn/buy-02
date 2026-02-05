package ax.gritlab.buy_01.order.controller;

import ax.gritlab.buy_01.order.model.Cart;
import ax.gritlab.buy_01.order.model.CartItem;
import ax.gritlab.buy_01.order.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
//@CrossOrigin(origins = "http://localhost:4200")
public class CartController {

   private final CartService cartService;

   @GetMapping("/{userId}")
   public ResponseEntity<Cart> getCart(@PathVariable String userId) {
      return ResponseEntity.ok(cartService.getCart(userId));
   }

   @PostMapping("/{userId}/items")
   public ResponseEntity<Cart> addItem(@PathVariable String userId, @RequestBody CartItem item) {
      return ResponseEntity.ok(cartService.addItem(userId, item));
   }

   @PutMapping("/{userId}/items/{productId}")
   public ResponseEntity<Cart> updateItemQuantity(
         @PathVariable String userId,
         @PathVariable String productId,
         @RequestParam int quantity) {
      return ResponseEntity.ok(cartService.updateItemQuantity(userId, productId, quantity));
   }

   @DeleteMapping("/{userId}/items/{productId}")
   public ResponseEntity<Cart> removeItem(
         @PathVariable String userId,
         @PathVariable String productId) {
      return ResponseEntity.ok(cartService.removeItem(userId, productId));
   }

   @DeleteMapping("/{userId}")
   public ResponseEntity<Void> clearCart(@PathVariable String userId) {
      cartService.clearCart(userId);
      return ResponseEntity.noContent().build();
   }
}
