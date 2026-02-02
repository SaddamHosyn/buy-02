package ax.gritlab.buy_01.order.config;

import ax.gritlab.buy_01.order.model.Cart;
import ax.gritlab.buy_01.order.model.Order;
import ax.gritlab.buy_01.order.model.Wishlist;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;

/**
 * MongoDB configuration for Order service.
 * 
 * Creates indexes programmatically to ensure they exist.
 * Some indexes are also defined via annotations on entities.
 */
@Configuration
public class MongoConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoConfig.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Initialize indexes on application startup.
     * This ensures all required indexes exist for optimal query performance.
     */
    @PostConstruct
    public void initIndexes() {
        try {
            createOrderIndexes();
            createCartIndexes();
            createWishlistIndexes();
            log.info("MongoDB indexes initialized successfully");
        } catch (Exception e) {
            // Log but don't fail startup - indexes may already exist with different names
            log.warn("Index creation warning (indexes may already exist): {}", e.getMessage());
        }
    }

    private void createOrderIndexes() {
        try {
            // Compound index for buyer order history (most common query)
            mongoTemplate.indexOps(Order.class).ensureIndex(
                new Index()
                    .on("buyerId", Sort.Direction.ASC)
                    .on("createdAt", Sort.Direction.DESC)
                    .named("buyer_orders_idx")
            );
        } catch (Exception e) {
            log.debug("Index buyer_orders_idx: {}", e.getMessage());
        }

        try {
            // Compound index for seller order queries
            mongoTemplate.indexOps(Order.class).ensureIndex(
                new Index()
                    .on("sellerIds", Sort.Direction.ASC)
                    .on("status", Sort.Direction.ASC)
                    .on("createdAt", Sort.Direction.DESC)
                    .named("seller_orders_idx")
            );
        } catch (Exception e) {
            log.debug("Index seller_orders_idx: {}", e.getMessage());
        }

        try {
            // Index for status-based queries
            mongoTemplate.indexOps(Order.class).ensureIndex(
                new Index()
                    .on("status", Sort.Direction.ASC)
                    .on("createdAt", Sort.Direction.DESC)
                    .named("status_orders_idx")
            );
        } catch (Exception e) {
            log.debug("Index status_orders_idx: {}", e.getMessage());
        }

        try {
            // Text index for order search (order number, buyer email)
            mongoTemplate.indexOps(Order.class).ensureIndex(
                new TextIndexDefinition.TextIndexDefinitionBuilder()
                    .onField("orderNumber")
                    .onField("buyerEmail")
                    .onField("buyerName")
                    .named("order_text_search_idx")
                    .build()
            );
        } catch (Exception e) {
            log.debug("Index order_text_search_idx: {}", e.getMessage());
        }

        try {
            // Index for soft-deleted orders cleanup
            mongoTemplate.indexOps(Order.class).ensureIndex(
                new Index()
                    .on("isRemoved", Sort.Direction.ASC)
                    .on("removedAt", Sort.Direction.ASC)
                    .named("removed_orders_idx")
            );
        } catch (Exception e) {
            log.debug("Index removed_orders_idx: {}", e.getMessage());
        }

        try {
            // Index for order lookup by original order (redo feature)
            mongoTemplate.indexOps(Order.class).ensureIndex(
                new Index()
                    .on("originalOrderId", Sort.Direction.ASC)
                    .sparse()
                    .named("original_order_idx")
            );
        } catch (Exception e) {
            log.debug("Index original_order_idx: {}", e.getMessage());
        }
    }

    private void createCartIndexes() {
        try {
            // Primary index on userId (unique - one cart per user)
            mongoTemplate.indexOps(Cart.class).ensureIndex(
                new Index()
                    .on("userId", Sort.Direction.ASC)
                    .unique()
                    .named("cart_user_idx")
            );
        } catch (Exception e) {
            log.debug("Index cart_user_idx: {}", e.getMessage());
        }

        try {
            // Index for abandoned cart cleanup
            mongoTemplate.indexOps(Cart.class).ensureIndex(
                new Index()
                    .on("updatedAt", Sort.Direction.ASC)
                    .named("cart_updated_idx")
            );
        } catch (Exception e) {
            log.debug("Index cart_updated_idx: {}", e.getMessage());
        }

        try {
            // Index for finding carts with specific products
            mongoTemplate.indexOps(Cart.class).ensureIndex(
                new Index()
                    .on("items.productId", Sort.Direction.ASC)
                    .named("cart_products_idx")
            );
        } catch (Exception e) {
            log.debug("Index cart_products_idx: {}", e.getMessage());
        }

        try {
            // Index for finding carts with products from specific seller
            mongoTemplate.indexOps(Cart.class).ensureIndex(
                new Index()
                    .on("items.sellerId", Sort.Direction.ASC)
                    .named("cart_sellers_idx")
            );
        } catch (Exception e) {
            log.debug("Index cart_sellers_idx: {}", e.getMessage());
        }

        try {
            // Index for cart status filtering
            mongoTemplate.indexOps(Cart.class).ensureIndex(
                new Index()
                    .on("status", Sort.Direction.ASC)
                    .named("cart_status_idx")
            );
        } catch (Exception e) {
            log.debug("Index cart_status_idx: {}", e.getMessage());
        }
    }

    private void createWishlistIndexes() {
        try {
            // Primary index on userId (unique - one wishlist per user)
            mongoTemplate.indexOps(Wishlist.class).ensureIndex(
                new Index()
                    .on("userId", Sort.Direction.ASC)
                    .unique()
                    .named("wishlist_user_idx")
            );
        } catch (Exception e) {
            log.debug("Index wishlist_user_idx: {}", e.getMessage());
        }

        try {
            // Index for finding wishlists containing specific products
            mongoTemplate.indexOps(Wishlist.class).ensureIndex(
                new Index()
                    .on("items.productId", Sort.Direction.ASC)
                    .named("wishlist_products_idx")
            );
        } catch (Exception e) {
            log.debug("Index wishlist_products_idx: {}", e.getMessage());
        }

        try {
            // Index for finding wishlists with products from specific seller
            mongoTemplate.indexOps(Wishlist.class).ensureIndex(
                new Index()
                    .on("items.sellerId", Sort.Direction.ASC)
                    .named("wishlist_sellers_idx")
            );
        } catch (Exception e) {
            log.debug("Index wishlist_sellers_idx: {}", e.getMessage());
        }
    }
}
