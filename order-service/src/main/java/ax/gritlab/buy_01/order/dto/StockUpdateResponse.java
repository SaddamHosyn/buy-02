package ax.gritlab.buy_01.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.ArrayList;

/**
 * Response DTO for batch stock update operations.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockUpdateResponse {

    private boolean success;
    private String message;
    
    @Builder.Default
    private List<StockUpdateResult> results = new ArrayList<>();

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StockUpdateResult {
        private String productId;
        private String productName;
        private boolean success;
        private String error;
        private Integer previousStock;
        private Integer newStock;
    }
}
