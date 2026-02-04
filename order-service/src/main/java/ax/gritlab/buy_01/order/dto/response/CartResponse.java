package ax.gritlab.buy_01.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartResponse {
    private String id;
    private String userId;
    private String status;
    private List<CartItemResponse> items;
    private Integer totalItems;
    private Double cachedSubtotal;
    private String message; // For redo operation
}
