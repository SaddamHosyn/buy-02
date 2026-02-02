package ax.gritlab.buy_01.order.model;

/**
 * Cart status enum representing the lifecycle of a shopping cart.
 */
public enum CartStatus {
    /**
     * Active cart - user is still shopping.
     */
    ACTIVE,

    /**
     * Cart has been checked out and converted to an order.
     */
    PURCHASED,

    /**
     * Cart was abandoned (no activity for extended period).
     */
    ABANDONED,

    /**
     * Cart was merged with another cart (e.g., guest to logged-in).
     */
    MERGED
}
