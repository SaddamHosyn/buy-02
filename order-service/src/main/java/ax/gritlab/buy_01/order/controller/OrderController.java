package ax.gritlab.buy_01.order.controller;

import ax.gritlab.buy_01.order.model.BuyerStats;
import ax.gritlab.buy_01.order.model.Order;
import ax.gritlab.buy_01.order.model.SellerStats;
import ax.gritlab.buy_01.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/test")
    public String test() {
        return "Order Service is running";
    }

    /**
     * Get orders for the current user (Buyer).
     * In a real app, userId comes from JWT/SecurityContext.
     * Here we pass it as a header for simplicity or mock it.
     */
    @GetMapping("/my-orders")
    public ResponseEntity<List<Order>> getMyOrders(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(orderService.getKeyOrdersForBuyer(userId));
    }

    /**
     * Get stats for the buyer dashboard.
     */
    @GetMapping("/stats/buyer")
    public ResponseEntity<BuyerStats> getBuyerStats(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(orderService.getBuyerStats(userId));
    }

    /**
     * Get stats for the seller dashboard.
     */
    @GetMapping("/stats/seller")
    public ResponseEntity<SellerStats> getSellerStats(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(orderService.getSellerStats(userId));
    }
}
