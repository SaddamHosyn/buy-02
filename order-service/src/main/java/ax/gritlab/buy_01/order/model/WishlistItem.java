package ax.gritlab.buy_01.order.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a single item saved in user's wishlist.
 * 
 * This is an embedded document within the Wishlist collection.
 * Unlike CartItem, this is just a reference - no quantity needed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItem {

    /**
     * Reference to the product.
     */
    @NotNull
    private String productId;

    /**
     * ID of the seller who owns this product.
     * Stored for quick filtering.
     */
    private String sellerId;

    /**
     * When this item was added to wishlist.
     */
    private LocalDateTime addedAt;

    /**
     * Cached product name for display.
     * Should be refreshed when wishlist is viewed.
     */
    private String cachedProductName;

    /**
     * Cached price for display.
     */
    private Double cachedPrice;

    /**
     * Cached availability status.
     */
    private Boolean cachedInStock;

    /**
     * Optional note from user about this item.
     */
    private String note;

    /**
     * Priority level (for sorting wishlist).
     * 1 = High, 2 = Medium, 3 = Low
     */
    @Builder.Default
    private Integer priority = 2;
}
