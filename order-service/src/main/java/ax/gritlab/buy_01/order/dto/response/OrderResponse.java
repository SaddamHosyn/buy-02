package ax.gritlab.buy_01.order.dto.response;

import ax.gritlab.buy_01.order.model.OrderStatus;
import ax.gritlab.buy_01.order.model.ShippingAddress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
    private String id;
    private String orderNumber;
    private String buyerId;
    private String buyerName;
    private String buyerEmail;
    private List<OrderItemResponse> items;
    private Double subtotal;
    private Double shippingCost;
    private Double taxAmount;
    private Double discountAmount;
    private Double totalAmount;
    private String paymentMethod;
    private String paymentStatus;
    private ShippingAddress shippingAddress;
    private String deliveryNotes;
    private OrderStatus status;
    private LocalDateTime estimatedDeliveryDate;
    private LocalDateTime actualDeliveryDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
