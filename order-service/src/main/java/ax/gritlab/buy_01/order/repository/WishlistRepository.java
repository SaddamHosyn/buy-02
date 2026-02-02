package ax.gritlab.buy_01.order.repository;

import ax.gritlab.buy_01.order.model.Wishlist;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Wishlist entity.
 * 
 * Supports:
 * - User wishlist retrieval
 * - Product existence checks
 * - Seller product wishlisting stats
 */
@Repository
public interface WishlistRepository extends MongoRepository<Wishlist, String> {

    /**
     * Find wishlist by user ID.
     */
    Optional<Wishlist> findByUserId(String userId);

    /**
     * Check if user has a wishlist.
     */
    boolean existsByUserId(String userId);

    /**
     * Delete wishlist by user ID.
     */
    void deleteByUserId(String userId);

    /**
     * Find all wishlists that contain a specific product.
     * Useful for sellers to see how many users wishlisted their product.
     */
    @Query("{ 'items.productId': ?0 }")
    List<Wishlist> findByProductId(String productId);

    /**
     * Count how many users have wishlisted a specific product.
     * Useful for "X users have this in their wishlist" feature.
     */
    @Query(value = "{ 'items.productId': ?0 }", count = true)
    long countByProductId(String productId);

    /**
     * Find all wishlists containing products from a specific seller.
     */
    @Query("{ 'items.sellerId': ?0 }")
    List<Wishlist> findBySellerId(String sellerId);
}
