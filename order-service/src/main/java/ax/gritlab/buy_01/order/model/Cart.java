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
 * Shopping Cart entity.
 * 
 * Design Decisions:
 * - One cart per user (userId is unique indexed)
 * - Cart persists across sessions (not session-based)
 * - Items reference products, not snapshot them (live pricing)
 * - At checkout, cart items are converted to order items with snapshots
 * 
 * Cart Lifecycle:
 * 1. Created when user adds first item
 * 2. Updated as user adds/removes/modifies items
 * 3. Cleared after successful checkout
 * 4. Can be abandoned (old carts may be cleaned up)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "carts")
public class Cart {

    @Id
    private String id;

    /**
     * Owner of this cart. One cart per user.
     */
    @NotNull
    @Indexed(unique = true)
    private String userId;

    /**
     * Cart status: ACTIVE, PURCHASED, ABANDONED, MERGED.
     * Required field for tracking cart lifecycle.
     */
    @NotNull
    @Builder.Default
    @Indexed
    private CartStatus status = CartStatus.ACTIVE;

    /**
     * Items currently in the cart.
     */
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    /**
     * Total number of items (sum of quantities).
     * Denormalized for quick badge display.
     */
    @Builder.Default
    private Integer totalItems = 0;

    /**
     * Cached subtotal for display.
     * Note: Recalculated fresh at checkout with live prices.
     */
    @Builder.Default
    private Double cachedSubtotal = 0.0;

    /**
     * When the cart was created.
     */
    @CreatedDate
    private LocalDateTime createdAt;

    /**
     * When the cart was last modified.
     */
    @LastModifiedDate
    @Indexed
    private LocalDateTime updatedAt;

    /**
     * When the cart was last accessed/viewed.
     * Used for abandoned cart cleanup.
     */
    private LocalDateTime lastAccessedAt;

    // ==================== Helper Methods ====================

    /**
     * Adds an item to cart or updates quantity if already exists.
     */
    public void addItem(CartItem newItem) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }

        Optional<CartItem> existing = findItemByProductId(newItem.getProductId());
        
        if (existing.isPresent()) {
            existing.get().incrementQuantity(newItem.getQuantity());
        } else {
            this.items.add(newItem);
        }
        
        recalculateTotals();
    }

    /**
     * Updates quantity of an existing item.
     */
    public boolean updateItemQuantity(String productId, int newQuantity) {
        Optional<CartItem> item = findItemByProductId(productId);
        
        if (item.isPresent()) {
            if (newQuantity <= 0) {
                removeItem(productId);
            } else {
                item.get().updateQuantity(newQuantity);
                recalculateTotals();
            }
            return true;
        }
        return false;
    }

    /**
     * Removes an item from the cart.
     */
    public boolean removeItem(String productId) {
        boolean removed = this.items.removeIf(item -> item.getProductId().equals(productId));
        if (removed) {
            recalculateTotals();
        }
        return removed;
    }

    /**
     * Clears all items from the cart.
     */
    public void clear() {
        this.items = new ArrayList<>();
        this.totalItems = 0;
        this.cachedSubtotal = 0.0;
    }

    /**
     * Finds an item by product ID.
     */
    public Optional<CartItem> findItemByProductId(String productId) {
        if (this.items == null) return Optional.empty();
        return this.items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();
    }

    /**
     * Recalculates total items count.
     */
    public void recalculateTotals() {
        if (this.items == null || this.items.isEmpty()) {
            this.totalItems = 0;
            this.cachedSubtotal = 0.0;
            return;
        }

        this.totalItems = this.items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        // Cached subtotal uses cached prices (actual price fetched at checkout)
        this.cachedSubtotal = this.items.stream()
                .filter(item -> item.getCachedPrice() != null)
                .mapToDouble(item -> item.getCachedPrice() * item.getQuantity())
                .sum();
    }

    /**
     * Checks if cart is empty.
     */
    public boolean isEmpty() {
        return this.items == null || this.items.isEmpty();
    }

    /**
     * Gets count of distinct products in cart.
     */
    public int getDistinctProductCount() {
        return this.items == null ? 0 : this.items.size();
    }

    /**
     * Updates last accessed timestamp.
     */
    public void markAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * Factory method to create a new cart for a user.
     */
    public static Cart createForUser(String userId) {
        return Cart.builder()
                .userId(userId)
                .items(new ArrayList<>())
                .totalItems(0)
                .cachedSubtotal(0.0)
                .lastAccessedAt(LocalDateTime.now())
                .build();
    }
}
