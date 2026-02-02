package ax.gritlab.buy_01.order.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a status change in the order's lifecycle.
 * Used for audit trail and order history tracking.
 * 
 * This is an embedded document within the Order collection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistory {

    /**
     * The previous status before this change.
     * Null for the first entry (order creation).
     */
    private OrderStatus previousStatus;

    /**
     * The new status after this change.
     */
    private OrderStatus newStatus;

    /**
     * Timestamp when the status change occurred.
     */
    private LocalDateTime changedAt;

    /**
     * ID of the user who triggered this status change.
     * Could be the buyer, seller, or system.
     */
    private String changedBy;

    /**
     * Role of the user who made the change (CLIENT, SELLER, SYSTEM).
     */
    private String changedByRole;

    /**
     * Optional reason/note for the status change.
     * Especially useful for cancellations and returns.
     */
    private String reason;

    /**
     * Factory method to create initial order status history entry.
     */
    public static OrderStatusHistory createInitial(String userId, String role) {
        return OrderStatusHistory.builder()
                .previousStatus(null)
                .newStatus(OrderStatus.PENDING)
                .changedAt(LocalDateTime.now())
                .changedBy(userId)
                .changedByRole(role)
                .reason("Order created")
                .build();
    }

    /**
     * Factory method to create a status transition entry.
     */
    public static OrderStatusHistory createTransition(
            OrderStatus from,
            OrderStatus to,
            String userId,
            String role,
            String reason) {
        return OrderStatusHistory.builder()
                .previousStatus(from)
                .newStatus(to)
                .changedAt(LocalDateTime.now())
                .changedBy(userId)
                .changedByRole(role)
                .reason(reason)
                .build();
    }
}
