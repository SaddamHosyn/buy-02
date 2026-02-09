package ax.gritlab.buy_01.order.service;

import ax.gritlab.buy_01.order.dto.request.AddToCartRequest;
import ax.gritlab.buy_01.order.dto.response.CartItemResponse;
import ax.gritlab.buy_01.order.dto.response.CartResponse;
import ax.gritlab.buy_01.order.exception.CartNotFoundException;
import ax.gritlab.buy_01.order.exception.CheckoutValidationException;
import ax.gritlab.buy_01.order.model.Cart;
import ax.gritlab.buy_01.order.model.CartItem;
import ax.gritlab.buy_01.order.model.CartStatus;
import ax.gritlab.buy_01.order.repository.CartRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final RestTemplate restTemplate;

    @Value("${product.service.url}")
    private String productServiceUrl;

    /**
     * Get user's cart. Creates one if it doesn't exist.
     */
    public CartResponse getCart(String userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createNewCart(userId));
        
        // Refresh cached prices
        refreshCartPrices(cart);
        
        return toCartResponse(cart);
    }

    /**
     * Add item to cart or update quantity if already exists.
     */
    public CartResponse addItem(String userId, AddToCartRequest request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createNewCart(userId));

        // Reset cart to ACTIVE if it was PURCHASED (user is starting a new shopping session)
        if (cart.getStatus() == CartStatus.PURCHASED) {
            cart.setStatus(CartStatus.ACTIVE);
        }

        // Fetch product details
        JsonNode product = fetchProductDetails(request.getProductId());
        
        if (product == null) {
            throw new CheckoutValidationException("Product not found with id: " + request.getProductId());
        }

        String sellerId = product.get("sellerId").asText();
        String productName = product.get("name").asText();
        Double price = product.get("price").asDouble();
        int availableStock = product.has("quantity") ? product.get("quantity").asInt() : 
                            (product.has("stock") ? product.get("stock").asInt() : Integer.MAX_VALUE);

        // Check if item already in cart
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst();

        // Calculate total quantity (existing + new)
        int currentQtyInCart = existingItem.map(CartItem::getQuantity).orElse(0);
        int totalRequestedQty = currentQtyInCart + request.getQuantity();
        
        // Validate against available stock
        if (totalRequestedQty > availableStock) {
            throw new CheckoutValidationException(
                String.format("Cannot add %d of '%s' to cart. Available stock: %d, Already in cart: %d",
                    request.getQuantity(), productName, availableStock, currentQtyInCart));
        }

        if (existingItem.isPresent()) {
            // Update existing item quantity
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            item.setUpdatedAt(LocalDateTime.now());
            item.setCachedPrice(price);
        } else {
            // Add new item
            CartItem newItem = CartItem.builder()
                    .productId(request.getProductId())
                    .quantity(request.getQuantity())
                    .sellerId(sellerId)
                    .addedAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .cachedProductName(productName)
                    .cachedPrice(price)
                    .build();
            cart.getItems().add(newItem);
        }

        cart.setUpdatedAt(LocalDateTime.now());
        cart.setLastAccessedAt(LocalDateTime.now());
        recalculateCartTotals(cart);
        
        Cart saved = cartRepository.save(cart);
        return toCartResponse(saved);
    }

    /**
     * Update item quantity in cart.
     */
    public CartResponse updateItem(String userId, String productId, Integer quantity) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for user"));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new CheckoutValidationException("Product not in cart"));

        // Validate against available stock
        JsonNode product = fetchProductDetails(productId);
        if (product != null) {
            int availableStock = product.has("quantity") ? product.get("quantity").asInt() : 
                                (product.has("stock") ? product.get("stock").asInt() : Integer.MAX_VALUE);
            String productName = product.has("name") ? product.get("name").asText() : productId;
            
            if (quantity > availableStock) {
                throw new CheckoutValidationException(
                    String.format("Cannot set quantity to %d for '%s'. Available stock: %d",
                        quantity, productName, availableStock));
            }
        }

        item.setQuantity(quantity);
        item.setUpdatedAt(LocalDateTime.now());
        
        cart.setUpdatedAt(LocalDateTime.now());
        recalculateCartTotals(cart);
        
        Cart saved = cartRepository.save(cart);
        return toCartResponse(saved);
    }

    /**
     * Remove item from cart.
     */
    public CartResponse removeItem(String userId, String productId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for user"));

        cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        
        cart.setUpdatedAt(LocalDateTime.now());
        recalculateCartTotals(cart);
        
        Cart saved = cartRepository.save(cart);
        return toCartResponse(saved);
    }

    /**
     * Clear all items from cart.
     */
    public void clearCart(String userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for user"));

        cart.getItems().clear();
        cart.setTotalItems(0);
        cart.setCachedSubtotal(0.0);
        cart.setUpdatedAt(LocalDateTime.now());
        
        cartRepository.save(cart);
    }

    /**
     * Validate cart before checkout - check stock and product availability.
     */
    public void validateCartForCheckout(Cart cart) {
        if (cart.getItems().isEmpty()) {
            throw new CheckoutValidationException("Cart is empty");
        }

        for (CartItem item : cart.getItems()) {
            JsonNode product = fetchProductDetails(item.getProductId());
            
            if (product == null) {
                throw new CheckoutValidationException(
                    "Product '" + item.getCachedProductName() + "' is no longer available"
                );
            }

            Integer availableStock = product.get("stock").asInt();
            if (availableStock < item.getQuantity()) {
                throw new CheckoutValidationException(
                    "Insufficient stock for '" + item.getCachedProductName() + 
                    "'. Available: " + availableStock + ", Requested: " + item.getQuantity()
                );
            }
        }
    }

    /**
     * Mark cart as purchased and clear items.
     */
    public void markCartAsPurchased(String userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for user"));

        cart.setStatus(CartStatus.PURCHASED);
        cart.getItems().clear();
        cart.setTotalItems(0);
        cart.setCachedSubtotal(0.0);
        cart.setUpdatedAt(LocalDateTime.now());
        
        cartRepository.save(cart);
    }

    // ==================== Helper Methods ====================

    private Cart createNewCart(String userId) {
        Cart cart = Cart.builder()
                .userId(userId)
                .status(CartStatus.ACTIVE)
                .items(new ArrayList<>())
                .totalItems(0)
                .cachedSubtotal(0.0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .lastAccessedAt(LocalDateTime.now())
                .build();
        return cartRepository.save(cart);
    }

    private void recalculateCartTotals(Cart cart) {
        int totalItems = cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
        
        double subtotal = cart.getItems().stream()
                .mapToDouble(item -> item.getCachedPrice() * item.getQuantity())
                .sum();

        cart.setTotalItems(totalItems);
        cart.setCachedSubtotal(subtotal);
    }

    private void refreshCartPrices(Cart cart) {
        for (CartItem item : cart.getItems()) {
            JsonNode product = fetchProductDetails(item.getProductId());
            if (product != null) {
                item.setCachedPrice(product.get("price").asDouble());
                item.setCachedProductName(product.get("name").asText());
            }
        }
        recalculateCartTotals(cart);
        cartRepository.save(cart);
    }

    private JsonNode fetchProductDetails(String productId) {
        try {
            String url = productServiceUrl + "/" + productId;
            System.out.println("Fetching product from: " + url);
            JsonNode result = restTemplate.getForObject(url, JsonNode.class);
            System.out.println("Product result: " + result);
            return result;
        } catch (Exception e) {
            System.err.println("Error fetching product: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private CartResponse toCartResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::toCartItemResponse)
                .collect(Collectors.toList());

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .status(cart.getStatus().name())
                .items(items)
                .totalItems(cart.getTotalItems())
                .cachedSubtotal(cart.getCachedSubtotal())
                .build();
    }

    private CartItemResponse toCartItemResponse(CartItem item) {
        return CartItemResponse.builder()
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .sellerId(item.getSellerId())
                .addedAt(item.getAddedAt())
                .updatedAt(item.getUpdatedAt())
                .cachedProductName(item.getCachedProductName())
                .cachedPrice(item.getCachedPrice())
                .build();
    }

    public Cart getCartEntity(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for user"));
    }
}
