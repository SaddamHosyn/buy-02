package ax.gritlab.buy_01.order.controller;

import ax.gritlab.buy_01.order.model.BuyerStats;
import ax.gritlab.buy_01.order.model.Order;
import ax.gritlab.buy_01.order.model.SellerStats;
import ax.gritlab.buy_01.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import ax.gritlab.buy_01.order.model.*;
import ax.gritlab.buy_01.order.repository.CartRepository;
import ax.gritlab.buy_01.order.repository.OrderRepository;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
//@CrossOrigin(origins = "http://localhost:4200")
public class OrderController {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final OrderService orderService;

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, String>> checkout(@RequestBody CheckoutRequest request) {
        // Validate request
        if (request.getUserId() == null || request.getShippingAddress() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID and shipping address are required");
        }

        // For demo purposes, return success with order number
        Map<String, String> response = new HashMap<>();
        response.put("message", "Order placed successfully");
        response.put("orderNumber", "ORD-" + System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

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

    @GetMapping("/my-orders/{userId}")
    public ResponseEntity<List<Order>> getOrdersByUserId(@PathVariable String userId) {
        List<Order> orders = orderRepository.findByBuyerIdAndNotRemoved(
                userId,
                PageRequest.of(0, 100, Sort.by("createdAt").descending())).getContent();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/seller-orders/{sellerId}")
    public ResponseEntity<List<Order>> getSellerOrders(@PathVariable String sellerId) {
        List<Order> orders = orderRepository.findBySellerIdInSellerIds(
                sellerId,
                PageRequest.of(0, 100, Sort.by("createdAt").descending())).getContent();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderById(@PathVariable String orderId) {
        return orderRepository.findById(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable String orderId,
            @RequestBody(required = false) CancelRequest cancelRequest) {

        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Order not found"));
        }

        Order order = orderOpt.get();

        // Validate cancellation is allowed
        if (!order.canBeCancelled()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Order cannot be cancelled",
                            "reason",
                            "Order status is " + order.getStatus()
                                    + ". Only PENDING, CONFIRMED, or PROCESSING orders can be cancelled.",
                            "currentStatus", order.getStatus().toString()));
        }

        // Require cancellation reason
        String reason = cancelRequest != null && cancelRequest.getReason() != null
                ? cancelRequest.getReason()
                : "Cancelled by customer";

        if (reason.trim().length() < 5) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Cancellation reason must be at least 5 characters"));
        }

        // Update order status with audit trail
        order.transitionStatus(OrderStatus.CANCELLED, order.getBuyerId(), "BUYER", reason);
        order.setUpdatedAt(LocalDateTime.now());

        // TODO: In real system, trigger:
        // - Refund processing
        // - Inventory restoration
        // - Seller notification
        // - Email notification to buyer

        order = orderRepository.save(order);

        return ResponseEntity.ok(Map.of(
                "message", "Order cancelled successfully",
                "order", order,
                "refundStatus", "Refund will be processed within 3-5 business days"));
    }

    @PostMapping("/{orderId}/redo")
    public ResponseEntity<?> redoOrder(@PathVariable String orderId) {
        Optional<Order> originalOrderOpt = orderRepository.findById(orderId);
        if (originalOrderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Original order not found"));
        }

        Order originalOrder = originalOrderOpt.get();

        // Only allow redo for CANCELLED orders
        if (!originalOrder.canBeRedone()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Order cannot be redone",
                            "reason",
                            "Only cancelled orders can be redone. Current status: " + originalOrder.getStatus()));
        }

        // Create new order from cancelled order
        Order newOrder = Order.builder()
                .orderNumber(Order.generateOrderNumber())
                .buyerId(originalOrder.getBuyerId())
                .buyerName(originalOrder.getBuyerName())
                .buyerEmail(originalOrder.getBuyerEmail())
                .items(new ArrayList<>(originalOrder.getItems()))
                .shippingAddress(originalOrder.getShippingAddress())
                .subtotal(originalOrder.getSubtotal())
                .shippingCost(originalOrder.getShippingCost())
                .taxAmount(originalOrder.getTaxAmount())
                .discountAmount(originalOrder.getDiscountAmount())
                .totalAmount(originalOrder.getTotalAmount())
                .paymentMethod(originalOrder.getPaymentMethod())
                .paymentStatus("PENDING")
                .status(OrderStatus.PENDING)
                .statusHistory(new ArrayList<>())
                .originalOrderId(originalOrder.getId())
                .isRemoved(false)
                .createdAt(LocalDateTime.now())
                .build();

        // Calculate totals and initialize status history
        newOrder.calculateTotals();
        newOrder.transitionStatus(OrderStatus.PENDING, newOrder.getBuyerId(), "BUYER",
                "Order recreated from cancelled order " + originalOrder.getOrderNumber());

        // TODO: In real system:
        // - Check product availability
        // - Verify current prices (may have changed)
        // - Check stock levels
        // - Validate seller is still active

        newOrder = orderRepository.save(newOrder);

        return ResponseEntity.ok(Map.of(
                "message", "New order created successfully",
                "order", newOrder,
                "originalOrderNumber", originalOrder.getOrderNumber(),
                "note", "Please review items and proceed to payment"));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<?> removeOrder(@PathVariable String orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Order not found"));
        }

        Order order = orderOpt.get();

        // Only allow removing CANCELLED, DELIVERED, or RETURNED orders
        List<OrderStatus> removableStatuses = List.of(
                OrderStatus.CANCELLED,
                OrderStatus.DELIVERED,
                OrderStatus.RETURNED);

        if (!removableStatuses.contains(order.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Order cannot be removed",
                            "reason",
                            "Only cancelled, delivered, or returned orders can be removed from your list. Current status: "
                                    + order.getStatus(),
                            "currentStatus", order.getStatus().toString(),
                            "allowedStatuses", removableStatuses.stream().map(Enum::toString).toList()));
        }

        // Soft delete - mark as removed
        order.setIsRemoved(true);
        order.setRemovedAt(LocalDateTime.now());
        order.setRemovedBy(order.getBuyerId());
        orderRepository.save(order);

        return ResponseEntity.ok(Map.of(
                "message", "Order removed from your list",
                "orderNumber", order.getOrderNumber(),
                "note", "Order has been archived and hidden from your orders list"));
    }

    @Data
    public static class CheckoutRequest {
        private String userId;
        private String paymentMethod;
        private ShippingAddress shippingAddress;
        private String deliveryNotes;
    }

    @Data
    public static class CancelRequest {
        private String reason;
    }
}
