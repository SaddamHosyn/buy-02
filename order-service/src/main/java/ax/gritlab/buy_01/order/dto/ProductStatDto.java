package ax.gritlab.buy_01.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for product statistics in profile.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductStatDto {
    
    /**
     * Product ID
     */
    private String productId;
    
    /**
     * Product name (from order items)
     */
    private String productName;
    
    /**
     * Total quantity purchased/sold
     */
    private Integer totalQuantity;
    
    /**
     * Total amount spent/gained on this product
     */
    private Double totalAmount;
    
    /**
     * Number of orders containing this product
     */
    private Integer orderCount;
    
    /**
     * Thumbnail media ID for display
     */
    private String thumbnailMediaId;
}
