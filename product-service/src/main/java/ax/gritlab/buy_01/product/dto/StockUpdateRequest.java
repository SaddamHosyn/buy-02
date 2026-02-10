package ax.gritlab.buy_01.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request DTO for batch stock updates during order checkout.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockUpdateRequest {

    @NotEmpty(message = "Items list cannot be empty")
    private List<StockUpdateItem> items;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StockUpdateItem {
        @NotNull(message = "Product ID is required")
        private String productId;

        @NotNull(message = "Quantity is required")
        private Integer quantity;
    }
}
