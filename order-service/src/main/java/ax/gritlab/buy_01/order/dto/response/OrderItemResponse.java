package ax.gritlab.buy_01.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemResponse {
    private String productId;
    private String productName;
    private String productDescription;
    private Double priceAtPurchase;
    private Integer quantity;
    private Double subtotal;
    private String sellerId;
    private String sellerName;
    private String thumbnailMediaId;
}
