package ax.gritlab.buy_01.order.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Main Order entity representing a completed purchase.
 * 
 * Design Decisions:
 * - Uses MongoDB for flexible document structure
 * - Embeds OrderItems with snapshotted product data for historical accuracy
 * - Maintains full status history for audit trail
 * - Indexes optimized for both user and seller queries
 * 
 * Snapshotted Fields (frozen at checkout):
 * - All OrderItem fields (product name, price, description, seller info)
 * - ShippingAddress
 * - Total amounts
 * 
 * Query Patterns Supported:
 * 1. User's orders (by buyerId + status + date range)
 * 2. Seller's orders (by sellerIds contains + status + date range)
 * 3. Order search by orderNumber
 * 4. Orders by status for management
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
@CompoundIndexes({
    // User order listing: find orders by buyer, sorted by date
    @CompoundIndex(name = "buyer_date_idx", def = "{'buyerId': 1, 'createdAt': -1}"),
    
    // Seller order listing: find orders containing seller's products
    @CompoundIndex(name = "seller_status_date_idx", def = "{'sellerIds': 1, 'status': 1, 'createdAt': -1}"),
    
    // Status-based queries for order management
    @CompoundIndex(name = "status_date_idx", def = "{'status': 1, 'createdAt': -1}"),
    
    // Text search on order number and buyer email
    @CompoundIndex(name = "search_idx", def = "{'orderNumber': 1, 'buyerEmail': 1}")
})
public class Order {

    @Id
    private String id;

    /**
     * Human-readable order number for customer reference.
     * Format: ORD-YYYYMMDD-XXXXX (e.g., ORD-20260201-A3F5K)
     */
    @NotNull
    @Indexed(unique = true)
    private String orderNumber;

    // ==================== Buyer Information ====================
    
    /**
     * ID of the user who placed this order.
     * Indexed for user order history queries.
     */
    @NotNull
    @Indexed
    private String buyerId;

    /**
     * SNAPSHOTTED: Buyer's name at time of purchase.
     */
    @NotNull
    private String buyerName;

    /**
     * SNAPSHOTTED: Buyer's email at time of purchase.
     * Used for order search.
     */
    @NotNull
    @Indexed
    private String buyerEmail;

    // ==================== Order Items ====================
    
    /**
     * List of items in this order with snapshotted product data.
     */
    @NotNull
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    /**
     * Set of seller IDs who have products in this order.
     * Denormalized for efficient seller queries.
     * Automatically populated from items.
     */
    @Builder.Default
    @Indexed
    private Set<String> sellerIds = new HashSet<>();

    // ==================== Pricing (Snapshotted) ====================
    
    /**
     * Sum of all item subtotals.
     */
    @NotNull
    private Double subtotal;

    /**
     * Shipping cost at time of checkout.
     */
    @Builder.Default
    private Double shippingCost = 0.0;

    /**
     * Tax amount at time of checkout.
     */
    @Builder.Default
    private Double taxAmount = 0.0;

    /**
     * Any discount applied at checkout.
     */
    @Builder.Default
    private Double discountAmount = 0.0;

    /**
     * Final total: subtotal + shipping + tax - discount
     */
    @NotNull
    private Double totalAmount;

    // ==================== Payment ====================
    
    /**
     * Payment method used. For buy-02, this is "PAY_ON_DELIVERY".
     */
    @NotNull
    @Builder.Default
    private String paymentMethod = "PAY_ON_DELIVERY";

    /**
     * Payment status: PENDING, COMPLETED, REFUNDED.
     */
    @Builder.Default
    private String paymentStatus = "PENDING";

    // ==================== Shipping ====================
    
    /**
     * SNAPSHOTTED: Delivery address at time of checkout.
     */
    private ShippingAddress shippingAddress;

    /**
     * Optional notes from buyer for delivery.
     */
    private String deliveryNotes;

    // ==================== Status & Tracking ====================
    
    /**
     * Current order status.
     */
    @NotNull
    @Indexed
    private OrderStatus status;

    /**
     * Complete history of status changes for audit.
     */
    @Builder.Default
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

    /**
     * Expected delivery date (estimated).
     */
    private LocalDateTime estimatedDeliveryDate;

    /**
     * Actual delivery date when completed.
     */
    private LocalDateTime actualDeliveryDate;

    // ==================== Audit Fields ====================
    
    /**
     * Timestamp when order was created.
     */
    @CreatedDate
    @Indexed
    private LocalDateTime createdAt;

    /**
     * Timestamp of last modification.
     */
    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * If this order was created from a cancelled order (redo).
     */
    private String originalOrderId;

    /**
     * Soft delete flag. Removed orders are hidden, not deleted.
     */
    @Builder.Default
    private Boolean isRemoved = false;

    /**
     * Timestamp when order was removed (soft delete).
     */
    private LocalDateTime removedAt;

    /**
     * User who removed the order.
     */
    private String removedBy;

    // ==================== Helper Methods ====================

    /**
     * Calculates totals from items and populates seller IDs.
     * Call this after adding items.
     */
    public void calculateTotals() {
        this.subtotal = 0.0;
        this.sellerIds = new HashSet<>();
        
        if (this.items != null) {
            for (OrderItem item : this.items) {
                item.calculateSubtotal();
                this.subtotal += item.getSubtotal();
                this.sellerIds.add(item.getSellerId());
            }
        }
        
        this.totalAmount = this.subtotal 
                + (this.shippingCost != null ? this.shippingCost : 0.0)
                + (this.taxAmount != null ? this.taxAmount : 0.0)
                - (this.discountAmount != null ? this.discountAmount : 0.0);
    }

    /**
     * Adds a status change to history and updates current status.
     */
    public void transitionStatus(OrderStatus newStatus, String userId, String role, String reason) {
        if (this.statusHistory == null) {
            this.statusHistory = new ArrayList<>();
        }
        
        OrderStatusHistory history = OrderStatusHistory.createTransition(
                this.status,
                newStatus,
                userId,
                role,
                reason
        );
        
        this.statusHistory.add(history);
        this.status = newStatus;
    }

    /**
     * Checks if order can be cancelled based on current status.
     */
    public boolean canBeCancelled() {
        return this.status == OrderStatus.PENDING 
            || this.status == OrderStatus.CONFIRMED 
            || this.status == OrderStatus.PROCESSING;
    }

    /**
     * Checks if order can be redone (recreated from cancelled).
     */
    public boolean canBeRedone() {
        return this.status == OrderStatus.CANCELLED;
    }

    /**
     * Generates order number with format: ORD-YYYYMMDD-XXXXX
     */
    public static String generateOrderNumber() {
        String datePart = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = java.util.UUID.randomUUID().toString()
                .substring(0, 5).toUpperCase();
        return "ORD-" + datePart + "-" + randomPart;
    }

    /**
     * Get total item count in order.
     */
    public int getTotalItemCount() {
        if (this.items == null) return 0;
        return this.items.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }
}
