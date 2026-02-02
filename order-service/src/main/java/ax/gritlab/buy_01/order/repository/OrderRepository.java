package ax.gritlab.buy_01.order.repository;

import ax.gritlab.buy_01.order.model.Order;
import ax.gritlab.buy_01.order.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Order entity with optimized queries for:
 * - User order history
 * - Seller order management
 * - Order search and filtering
 * - Statistics and analytics
 * 
 * Note: Complex aggregation queries for stats should use MongoTemplate.
 */
@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    // ==================== Find by Order Number ====================
    
    /**
     * Find order by unique order number.
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * Check if order number exists.
     */
    boolean existsByOrderNumber(String orderNumber);

    // ==================== Buyer Queries ====================
    
    /**
     * Find all orders for a buyer, sorted by date descending.
     * Excludes soft-deleted orders.
     */
    @Query("{ 'buyerId': ?0, 'isRemoved': { $ne: true } }")
    Page<Order> findByBuyerIdAndNotRemoved(String buyerId, Pageable pageable);

    /**
     * Find buyer's orders with specific status.
     */
    @Query("{ 'buyerId': ?0, 'status': ?1, 'isRemoved': { $ne: true } }")
    Page<Order> findByBuyerIdAndStatus(String buyerId, OrderStatus status, Pageable pageable);

    /**
     * Find buyer's orders within date range.
     */
    @Query("{ 'buyerId': ?0, 'createdAt': { $gte: ?1, $lte: ?2 }, 'isRemoved': { $ne: true } }")
    List<Order> findByBuyerIdAndDateRange(String buyerId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Search buyer's orders by order number (partial match).
     */
    @Query("{ 'buyerId': ?0, 'orderNumber': { $regex: ?1, $options: 'i' }, 'isRemoved': { $ne: true } }")
    Page<Order> searchByBuyerIdAndOrderNumber(String buyerId, String orderNumberPattern, Pageable pageable);

    /**
     * Count orders by buyer.
     */
    long countByBuyerId(String buyerId);

    // ==================== Seller Queries ====================
    
    /**
     * Find all orders containing products from a specific seller.
     * Uses the denormalized sellerIds field for efficiency.
     */
    @Query("{ 'sellerIds': ?0, 'isRemoved': { $ne: true } }")
    Page<Order> findBySellerIdInSellerIds(String sellerId, Pageable pageable);

    /**
     * Find seller's orders with specific status.
     */
    @Query("{ 'sellerIds': ?0, 'status': ?1, 'isRemoved': { $ne: true } }")
    Page<Order> findBySellerIdAndStatus(String sellerId, OrderStatus status, Pageable pageable);

    /**
     * Find seller's orders within date range.
     */
    @Query("{ 'sellerIds': ?0, 'createdAt': { $gte: ?1, $lte: ?2 }, 'isRemoved': { $ne: true } }")
    List<Order> findBySellerIdAndDateRange(String sellerId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Search seller's orders by order number or buyer email.
     */
    @Query("{ 'sellerIds': ?0, '$or': [ " +
           "{ 'orderNumber': { $regex: ?1, $options: 'i' } }, " +
           "{ 'buyerEmail': { $regex: ?1, $options: 'i' } } " +
           "], 'isRemoved': { $ne: true } }")
    Page<Order> searchBySellerIdAndKeyword(String sellerId, String keyword, Pageable pageable);

    /**
     * Count orders for seller.
     */
    @Query(value = "{ 'sellerIds': ?0 }", count = true)
    long countBySellerId(String sellerId);

    // ==================== Status Queries ====================
    
    /**
     * Find all orders with specific status.
     */
    Page<Order> findByStatusAndIsRemovedFalse(OrderStatus status, Pageable pageable);

    /**
     * Find all orders with status in list.
     */
    Page<Order> findByStatusInAndIsRemovedFalse(List<OrderStatus> statuses, Pageable pageable);

    /**
     * Count orders by status.
     */
    long countByStatus(OrderStatus status);

    // ==================== Date Range Queries ====================
    
    /**
     * Find orders created between dates.
     */
    @Query("{ 'createdAt': { $gte: ?0, $lte: ?1 }, 'isRemoved': { $ne: true } }")
    List<Order> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find recent orders (last N days).
     */
    @Query("{ 'createdAt': { $gte: ?0 }, 'isRemoved': { $ne: true } }")
    List<Order> findOrdersAfter(LocalDateTime since);

    // ==================== Original Order (Redo) Queries ====================
    
    /**
     * Find orders created from a cancelled order (redo).
     */
    List<Order> findByOriginalOrderId(String originalOrderId);

    // ==================== Aggregation Support (for stats) ====================
    
    /**
     * Find completed orders for buyer (for spending stats).
     */
    @Query("{ 'buyerId': ?0, 'status': 'DELIVERED' }")
    List<Order> findDeliveredOrdersByBuyer(String buyerId);

    /**
     * Find completed orders for seller (for earnings stats).
     */
    @Query("{ 'sellerIds': ?0, 'status': 'DELIVERED' }")
    List<Order> findDeliveredOrdersBySeller(String sellerId);
}
