package ax.gritlab.buy_01.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for buyer profile statistics.
 * Contains spending history and product preferences.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BuyerProfileStatsDto {
    
    /**
     * User ID of the buyer
     */
    private String userId;
    
    /**
     * Total amount spent on delivered orders
     */
    private Double totalSpent;
    
    /**
     * Total number of completed orders
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
     * Top products by purchase amount (most spent on)
     */
    private List<ProductStatDto> topProductsByAmount;
    
    /**
     * Most frequently purchased products (by quantity)
     */
    private List<ProductStatDto> mostBoughtProducts;
    
    /**
     * Average order value
     */
    private Double averageOrderValue;
}
