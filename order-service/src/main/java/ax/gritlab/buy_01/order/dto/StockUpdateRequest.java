package ax.gritlab.buy_01.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for batch stock updates during order checkout.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockUpdateRequest {

    private List<StockUpdateItem> items;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StockUpdateItem {
        private String productId;
        private Integer quantity;
    }
}
