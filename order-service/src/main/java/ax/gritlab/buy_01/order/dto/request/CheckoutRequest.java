package ax.gritlab.buy_01.order.dto.request;

import ax.gritlab.buy_01.order.model.ShippingAddress;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CheckoutRequest {
    
    @NotNull(message = "Shipping address is required")
    private ShippingAddress shippingAddress;
    
    private String deliveryNotes;
    
    @Builder.Default
    private String paymentMethod = "PAY_ON_DELIVERY";
}
