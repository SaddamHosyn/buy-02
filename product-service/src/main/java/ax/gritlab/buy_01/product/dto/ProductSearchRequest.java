package ax.gritlab.buy_01.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for product search and filter requests.
 * All fields are optional - null values are ignored in the query.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductSearchRequest {
    
    /**
     * Text search query - searches in name and description.
     */
    private String q;
    
    /**
     * Filter by category.
     */
    private String category;
    
    /**
     * Minimum price filter.
     */
    private Double minPrice;
    
    /**
     * Maximum price filter.
     */
    private Double maxPrice;
    
    /**
     * Filter by tags (matches any).
     */
    private List<String> tags;
    
    /**
     * Filter by stock availability.
     * true = only in-stock products (quantity > 0)
     */
    private Boolean inStock;
    
    /**
     * Filter by seller ID.
     */
    private String sellerId;
}
