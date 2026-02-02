package ax.gritlab.buy_01.product.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.TextScore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Product entity with indexes optimized for:
 * - Text search on name and description
 * - Price range filtering
 * - Seller product listing
 * - Sorting by date, price, name
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
@CompoundIndexes({
        // Seller's products sorted by date
        @CompoundIndex(name = "seller_date_idx", def = "{'userId': 1, 'createdAt': -1}"),
        // Price range queries with date sorting
        @CompoundIndex(name = "price_date_idx", def = "{'price': 1, 'createdAt': -1}"),
        // In-stock products (quantity > 0) sorted by date
        @CompoundIndex(name = "stock_date_idx", def = "{'quantity': 1, 'createdAt': -1}")
})
public class Product {
    @Id
    private String id;

    /**
     * Product name - text indexed for search.
     * Weight 3 gives it higher priority than description in search results.
     */
    @NotNull
    @Size(min = 2, max = 100)
    @TextIndexed(weight = 3)
    private String name;

    /**
     * Product description - text indexed for search.
     * Weight 1 (default) for lower priority than name.
     */
    @Size(max = 500)
    @TextIndexed(weight = 1)
    private String description;

    /**
     * Price - indexed for range queries and sorting.
     */
    @NotNull
    @Indexed
    private Double price;

    /**
     * Available quantity - indexed for in-stock filtering.
     */
    @NotNull
    @Indexed
    private Integer quantity;

    /**
     * Seller ID - indexed for seller product queries.
     */
    @NotNull
    @Indexed
    private String userId;

    /**
     * Optional category for filtering.
     * Examples: Electronics, Clothing, Home & Garden, Sports, Books
     */
    @Indexed
    private String category;

    /**
     * Tags for flexible filtering and search.
     * Examples: ["new", "sale", "featured", "eco-friendly"]
     */
    @Indexed
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Builder.Default
    private List<String> mediaIds = new ArrayList<>();

    /**
     * Creation timestamp - indexed for sorting by newest.
     */
    @CreatedDate
    @Indexed
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * Text search score - populated by MongoDB text search.
     * Used for relevance-based sorting.
     */
    @TextScore
    private Float score;
}
