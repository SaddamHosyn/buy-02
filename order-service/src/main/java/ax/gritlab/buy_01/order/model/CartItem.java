package ax.gritlab.buy_01.order.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a single item in the shopping cart.
 * 
 * Unlike OrderItem, CartItem references live product data.
 * Product details are fetched fresh when displaying cart
 * and snapshotted only at checkout time.
 * 
 * This is an embedded document within the Cart collection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    /**
     * Reference to the product.
     * Product details are fetched live from product-service.
     */
    @NotNull
    private String productId;

    /**
     * Quantity the user wants to purchase.
     * Must be validated against available stock at checkout.
     */
    @NotNull
    @Min(1)
    private Integer quantity;

    /**
     * ID of the seller who owns this product.
     * Stored for quick filtering/grouping.
     */
    @NotNull
    private String sellerId;

    /**
     * When this item was added to cart.
     * Used for cart cleanup of old items.
     */
    private LocalDateTime addedAt;

    /**
     * When this item was last updated (quantity change).
     */
    private LocalDateTime updatedAt;

    /**
     * Cached product name for display without extra API calls.
     * Should be refreshed when cart is viewed.
     */
    private String cachedProductName;

    /**
     * Cached price for display.
     * Note: Actual price is fetched fresh at checkout.
     */
    private Double cachedPrice;

    /**
     * Factory method to create a new cart item.
     */
    public static CartItem create(String productId, Integer quantity, String sellerId) {
        LocalDateTime now = LocalDateTime.now();
        return CartItem.builder()
                .productId(productId)
                .quantity(quantity)
                .sellerId(sellerId)
                .addedAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * Updates quantity and refresh timestamp.
     */
    public void updateQuantity(int newQuantity) {
        this.quantity = newQuantity;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Increments quantity by specified amount.
     */
    public void incrementQuantity(int amount) {
        this.quantity += amount;
        this.updatedAt = LocalDateTime.now();
    }
}
