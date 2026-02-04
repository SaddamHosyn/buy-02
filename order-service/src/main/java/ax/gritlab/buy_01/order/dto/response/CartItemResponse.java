package ax.gritlab.buy_01.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartItemResponse {
    private String productId;
    private Integer quantity;
    private String sellerId;
    private LocalDateTime addedAt;
    private LocalDateTime updatedAt;
    private String cachedProductName;
    private Double cachedPrice;
}
