package ax.gritlab.buy_01.order.service;

import ax.gritlab.buy_01.order.client.ProductClient;
import ax.gritlab.buy_01.order.dto.ProductDTO;
import ax.gritlab.buy_01.order.model.Cart;
import ax.gritlab.buy_01.order.model.CartItem;
import ax.gritlab.buy_01.order.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductClient productClient;

    public Cart getCart(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> createEmptyCart(userId));
    }

    public Cart addItem(String userId, CartItem item) {
        // Check inventory
        ProductDTO product = productClient.getProductById(item.getProductId());
        if (product != null) {
            // Check if product is already in cart to calculate total quantity
            Cart cart = getCart(userId);
            int currentQuantityInCart = cart.getItems().stream()
                    .filter(i -> i.getProductId().equals(item.getProductId()))
                    .mapToInt(CartItem::getQuantity)
                    .sum();
            
            if (currentQuantityInCart + item.getQuantity() > product.getStock()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Not enough stock. Available: " + product.getStock());
            }

            item.setAddedAt(LocalDateTime.now());
            item.setUpdatedAt(LocalDateTime.now());
            item.setSellerId(product.getSellerId()); // Ensure sellerId is correct
            
            cart.addItem(item);
            return cartRepository.save(cart);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
    }

    public Cart updateItemQuantity(String userId, String productId, int quantity) {
        Optional<Cart> cartOpt = cartRepository.findByUserId(userId);
        if (cartOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found");
        }
        
        // Check inventory
        ProductDTO product = productClient.getProductById(productId);
        if (product != null) {
            if (quantity > product.getStock()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Not enough stock. Available: " + product.getStock());
            }
        }

        Cart cart = cartOpt.get();
        cart.updateItemQuantity(productId, quantity);
        return cartRepository.save(cart);
    }

    public Cart removeItem(String userId, String productId) {
        Optional<Cart> cartOpt = cartRepository.findByUserId(userId);
        if (cartOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found");
        }

        Cart cart = cartOpt.get();
        cart.removeItem(productId);
        return cartRepository.save(cart);
    }

    public void clearCart(String userId) {
        cartRepository.findByUserId(userId).ifPresent(cartRepository::delete);
    }

    private Cart createEmptyCart(String userId) {
        Cart cart = new Cart();
        cart.setUserId(userId);
        return cart;
    }
}
