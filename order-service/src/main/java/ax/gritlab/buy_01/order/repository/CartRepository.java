package ax.gritlab.buy_01.order.repository;

import ax.gritlab.buy_01.order.model.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Cart entity.
 * 
 * Simple queries as cart operations are mostly by userId.
 * Complex operations handled via service layer.
 */
@Repository
public interface CartRepository extends MongoRepository<Cart, String> {

    /**
     * Find cart by user ID.
     * Primary query method - one cart per user.
     */
    Optional<Cart> findByUserId(String userId);

    /**
     * Check if user has a cart.
     */
    boolean existsByUserId(String userId);

    /**
     * Delete cart by user ID (after checkout or user deletion).
     */
    void deleteByUserId(String userId);

    /**
     * Find abandoned carts (not updated since specified date).
     * Used for cleanup jobs.
     */
    @Query("{ 'updatedAt': { $lt: ?0 } }")
    List<Cart> findAbandonedCarts(LocalDateTime cutoffDate);

    /**
     * Find carts not accessed since specified date.
     * Alternative cleanup criteria.
     */
    @Query("{ 'lastAccessedAt': { $lt: ?0 } }")
    List<Cart> findInactiveCarts(LocalDateTime cutoffDate);

    /**
     * Count carts containing a specific product.
     * Useful for product deletion warnings.
     */
    @Query(value = "{ 'items.productId': ?0 }", count = true)
    long countCartsContainingProduct(String productId);

    /**
     * Find carts containing a specific product.
     * Used when product is deleted to notify users or remove items.
     */
    @Query("{ 'items.productId': ?0 }")
    List<Cart> findCartsContainingProduct(String productId);

    /**
     * Find carts containing products from a specific seller.
     * Used when seller account is deleted.
     */
    @Query("{ 'items.sellerId': ?0 }")
    List<Cart> findCartsContainingSellerProducts(String sellerId);
}
