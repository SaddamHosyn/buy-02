package ax.gritlab.buy_01.order.model;

/**
 * Order status enum representing the lifecycle of an order.
 * 
 * Status Flow:
 * PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
 *        ↘ CANCELLED (from PENDING, CONFIRMED, or PROCESSING)
 * 
 * DELIVERED orders can be marked as RETURNED within return window.
 * CANCELLED orders can be REDONE (creates new order with same items).
 */
public enum OrderStatus {
    /**
     * Initial state when order is created (checkout completed).
     * Order can be cancelled by user or seller.
     */
    PENDING,

    /**
     * Seller has confirmed the order and will process it.
     * Order can still be cancelled.
     */
    CONFIRMED,

    /**
     * Order is being prepared for shipment.
     * Order can still be cancelled (with potential restocking).
     */
    PROCESSING,

    /**
     * Order has been shipped to customer.
     * Cannot be cancelled, must wait for delivery.
     */
    SHIPPED,

    /**
     * Order has been delivered to customer.
     * Final positive state. Can initiate return if within window.
     */
    DELIVERED,

    /**
     * Order was cancelled by user or seller.
     * Can be redone (creates a new order).
     */
    CANCELLED,

    /**
     * Order was returned by customer after delivery.
     * Products should be restocked by seller.
     */
    RETURNED
}
