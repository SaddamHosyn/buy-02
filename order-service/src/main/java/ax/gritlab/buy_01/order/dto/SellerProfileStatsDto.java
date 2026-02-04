package ax.gritlab.buy_01.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for seller profile statistics.
 * Contains earnings and sales data.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SellerProfileStatsDto {
    
    /**
     * User ID of the seller
     */
    private String sellerId;
    
    /**
     * Total amount earned from delivered orders
     */
    private Double totalEarned;
    
    /**
     * Total number of orders containing seller's products
     */
    private Integer totalOrders;
    
    /**
     * Number of pending orders
     */
    private Integer pendingOrders;
    
    /**
     * Number of delivered orders
     */
    private Integer deliveredOrders;
    
    /**
     * Number of cancelled orders
     */
    private Integer cancelledOrders;
    
    /**
     * Total products sold (sum of quantities)
     */
    private Integer totalProductsSold;
    
    /**
     * Best-selling products by amount (most revenue)
     */
    private List<ProductStatDto> bestSellingByAmount;
    
    /**
     * Best-selling products by quantity
     */
    private List<ProductStatDto> bestSellingByQuantity;
    
    /**
     * Average order value for seller's products
     */
    private Double averageOrderValue;
}
