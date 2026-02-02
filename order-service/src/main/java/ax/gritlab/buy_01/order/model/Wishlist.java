package ax.gritlab.buy_01.order.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Wishlist entity for saving products for future purchase.
 * 
 * Design Decisions:
 * - One wishlist per user (userId is unique indexed)
 * - Stores product IDs only (live data fetched when viewing)
 * - Items can be moved to cart when user is ready to buy
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "wishlists")
public class Wishlist {

    @Id
    private String id;

    /**
     * Owner of this wishlist. One wishlist per user.
     */
    @NotNull
    @Indexed(unique = true)
    private String userId;

    /**
     * List of saved product items.
     */
    @Builder.Default
    private List<WishlistItem> items = new ArrayList<>();

    /**
     * Total count of items in wishlist.
     */
    @Builder.Default
    private Integer totalItems = 0;

    /**
     * When the wishlist was created.
     */
    @CreatedDate
    private LocalDateTime createdAt;

    /**
     * When the wishlist was last modified.
     */
    @LastModifiedDate
    private LocalDateTime updatedAt;

    // ==================== Helper Methods ====================

    /**
     * Add a product to the wishlist if not already present.
     */
    public boolean addItem(String productId, String sellerId) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }

        // Check if already in wishlist
        if (containsProduct(productId)) {
            return false;
        }

        WishlistItem item = WishlistItem.builder()
                .productId(productId)
                .sellerId(sellerId)
                .addedAt(LocalDateTime.now())
                .build();

        this.items.add(item);
        recalculateTotals();
        return true;
    }

    /**
     * Remove a product from the wishlist.
     */
    public boolean removeItem(String productId) {
        if (this.items == null) {
            return false;
        }

        boolean removed = this.items.removeIf(item -> item.getProductId().equals(productId));
        if (removed) {
            recalculateTotals();
        }
        return removed;
    }

    /**
     * Check if a product is in the wishlist.
     */
    public boolean containsProduct(String productId) {
        if (this.items == null) {
            return false;
        }
        return this.items.stream()
                .anyMatch(item -> item.getProductId().equals(productId));
    }

    /**
     * Find an item by product ID.
     */
    public Optional<WishlistItem> findItemByProductId(String productId) {
        if (this.items == null) {
            return Optional.empty();
        }
        return this.items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();
    }

    /**
     * Clear all items from wishlist.
     */
    public void clear() {
        if (this.items != null) {
            this.items.clear();
        }
        this.totalItems = 0;
    }

    /**
     * Recalculate the total item count.
     */
    public void recalculateTotals() {
        this.totalItems = this.items != null ? this.items.size() : 0;
    }

    /**
     * Get list of all product IDs in wishlist.
     */
    public List<String> getProductIds() {
        if (this.items == null) {
            return new ArrayList<>();
        }
        return this.items.stream()
                .map(WishlistItem::getProductId)
                .toList();
    }
}
