package ax.gritlab.buy_01.order.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single item within an order.
 * Contains snapshotted product data at the time of purchase.
 * 
 * This is an embedded document within the Order collection.
 * 
 * IMPORTANT: Product details are SNAPSHOTTED at checkout time.
 * This ensures order history remains accurate even if products
 * are later modified or deleted.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    /**
     * Reference to the original product ID.
     * Used for linking back to product for reordering.
     */
    @NotNull
    private String productId;

    /**
     * SNAPSHOTTED: Product name at time of purchase.
     * Will not change even if product is renamed.
     */
    @NotNull
    private String productName;

    /**
     * SNAPSHOTTED: Product description at time of purchase.
     */
    private String productDescription;

    /**
     * SNAPSHOTTED: Price per unit at time of purchase.
     * Critical for accurate order totals and history.
     */
    @NotNull
    private Double priceAtPurchase;

    /**
     * Quantity of this product ordered.
     */
    @NotNull
    @Min(1)
    private Integer quantity;

    /**
     * Calculated subtotal for this line item.
     * = priceAtPurchase * quantity
     */
    private Double subtotal;

    /**
     * ID of the seller who owns/sold this product.
     * Used for seller order queries.
     */
    @NotNull
    private String sellerId;

    /**
     * SNAPSHOTTED: Seller name at time of purchase.
     * For display purposes in order history.
     */
    private String sellerName;

    /**
     * First media ID for product thumbnail in order history.
     * Snapshotted for consistent display.
     */
    private String thumbnailMediaId;

    /**
     * Calculates and sets the subtotal based on price and quantity.
     */
    public void calculateSubtotal() {
        if (this.priceAtPurchase != null && this.quantity != null) {
            this.subtotal = this.priceAtPurchase * this.quantity;
        }
    }

    /**
     * Factory method to create an OrderItem from cart data with snapshots.
     */
    public static OrderItem fromCartItem(
            String productId,
            String productName,
            String productDescription,
            Double currentPrice,
            Integer quantity,
            String sellerId,
            String sellerName,
            String thumbnailMediaId) {
        
        OrderItem item = OrderItem.builder()
                .productId(productId)
                .productName(productName)
                .productDescription(productDescription)
                .priceAtPurchase(currentPrice)
                .quantity(quantity)
                .sellerId(sellerId)
                .sellerName(sellerName)
                .thumbnailMediaId(thumbnailMediaId)
                .build();
        
        item.calculateSubtotal();
        return item;
    }
}
