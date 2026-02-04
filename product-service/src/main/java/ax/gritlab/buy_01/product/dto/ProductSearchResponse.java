package ax.gritlab.buy_01.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated response for product search results.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductSearchResponse {
    
    /**
     * List of products matching the search criteria.
     */
    private List<ProductResponse> products;
    
    /**
     * Current page number (0-indexed).
     */
    private int page;
    
    /**
     * Number of products per page.
     */
    private int size;
    
    /**
     * Total number of products matching criteria.
     */
    private long totalElements;
    
    /**
     * Total number of pages.
     */
    private int totalPages;
    
    /**
     * Whether this is the first page.
     */
    private boolean first;
    
    /**
     * Whether this is the last page.
     */
    private boolean last;
}
