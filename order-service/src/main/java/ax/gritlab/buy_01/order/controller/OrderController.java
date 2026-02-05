package ax.gritlab.buy_01.order.controller;

import ax.gritlab.buy_01.order.model.*;
import ax.gritlab.buy_01.order.repository.CartRepository;
import ax.gritlab.buy_01.order.repository.OrderRepository;
import ax.gritlab.buy_01.order.security.AuthenticatedUser;
import ax.gritlab.buy_01.order.security.AuthorizationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Order Controller with proper server-side authorization.
 * 
 * Implements three key security checks:
 * 1. User vs Seller capabilities - role-based actions
 * 2. Order ownership - users can only access their own orders
 * 3. Mutable status rules - status-based action validation
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class OrderController {

      private final OrderRepository orderRepository;
      private final CartRepository cartRepository;
      private final AuthorizationService authService;

      @PostMapping("/checkout")
      public ResponseEntity<Map<String, String>> checkout(@RequestBody CheckoutRequest request) {
            // Get authenticated user - no external userId needed
            AuthenticatedUser currentUser = authService.requireAuthentication();

            // Validate request
            if (request.getShippingAddress() == null) {
                  throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shipping address is required");
            }

            // Use authenticated user's ID for order creation
            // For demo purposes, return success with order number
            Map<String, String> response = new HashMap<>();
            response.put("message", "Order placed successfully");
            response.put("orderNumber", "ORD-" + System.currentTimeMillis());
            response.put("userId", currentUser.getUserId());
            return ResponseEntity.ok(response);
      }

      /**
       * Get orders for the authenticated buyer.
       * Server automatically uses the authenticated user's ID - no path variable
       * needed.
       */
      @GetMapping("/my-orders")
      public ResponseEntity<List<Order>> getMyOrders() {
            AuthenticatedUser currentUser = authService.requireAuthentication();

            List<Order> orders = orderRepository.findByBuyerIdAndNotRemoved(
                        currentUser.getUserId(),
                        PageRequest.of(0, 100, Sort.by("createdAt").descending())).getContent();
            return ResponseEntity.ok(orders);
      }

      /**
       * Get orders for the authenticated seller.
       * Only sellers can access this endpoint.
       * Returns orders containing the seller's products.
       */
      @GetMapping("/seller-orders")
      public ResponseEntity<List<Order>> getSellerOrders() {
            // Verify user is authenticated and is a seller
            authService.requireSellerRole();
            AuthenticatedUser currentUser = authService.getCurrentUser();

            List<Order> orders = orderRepository.findBySellerIdInSellerIds(
                        currentUser.getUserId(),
                        PageRequest.of(0, 100, Sort.by("createdAt").descending())).getContent();
            return ResponseEntity.ok(orders);
      }

      /**
       * Get order by ID.
       * User must be either the buyer or a seller with products in the order.
       */
      @GetMapping("/{orderId}")
      public ResponseEntity<Order> getOrderById(@PathVariable String orderId) {
            Order order = orderRepository.findById(orderId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

            // Validate access: user must be buyer or seller in this order
            authService.validateOrderAccess(order);

            return ResponseEntity.ok(order);
      }

      /**
       * Cancel an order.
       * Only the buyer can cancel their own order.
       * Order must be in a cancellable status (PENDING, CONFIRMED, PROCESSING).
       */
      @PutMapping("/{orderId}/cancel")
      public ResponseEntity<?> cancelOrder(
                  @PathVariable String orderId,
                  @RequestBody(required = false) CancelRequest cancelRequest) {

            Order order = orderRepository.findById(orderId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

            // Authorization: verify user owns this order
            authService.validateOrderOwnership(order);

            // Business rule: verify order can be cancelled
            authService.validateCanCancel(order);

            // Require cancellation reason
            String reason = cancelRequest != null && cancelRequest.getReason() != null
                        ? cancelRequest.getReason()
                        : "Cancelled by customer";

            if (reason.trim().length() < 5) {
                  throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                              "Cancellation reason must be at least 5 characters");
            }

            // Update order status with audit trail (using authenticated user info)
            AuthenticatedUser currentUser = authService.getCurrentUser();
            order.transitionStatus(OrderStatus.CANCELLED, currentUser.getUserId(),
                        currentUser.getRole(), reason);
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

      /**
       * Redo a cancelled order (creates new order with same items).
       * Only the original buyer can redo their cancelled order.
       */
      @PostMapping("/{orderId}/redo")
      public ResponseEntity<?> redoOrder(@PathVariable String orderId) {
            Order originalOrder = orderRepository.findById(orderId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                    "Original order not found"));

            // Authorization: verify user owns this order
            authService.validateOrderOwnership(originalOrder);

            // Business rule: verify order can be redone
            authService.validateCanRedo(originalOrder);

            // Get current user for audit trail
            AuthenticatedUser currentUser = authService.getCurrentUser();

            // Create new order from cancelled order
            Order newOrder = Order.builder()
                        .orderNumber(Order.generateOrderNumber())
                        .buyerId(currentUser.getUserId()) // Use authenticated user
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
            newOrder.transitionStatus(OrderStatus.PENDING, currentUser.getUserId(),
                        currentUser.getRole(),
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

      /**
       * Remove order (soft delete - hides from user's view).
       * Only the buyer can remove their own order.
       * Order must be in a final status (CANCELLED, DELIVERED, RETURNED).
       */
      @DeleteMapping("/{orderId}")
      public ResponseEntity<?> removeOrder(@PathVariable String orderId) {
            Order order = orderRepository.findById(orderId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

            // Authorization: verify user owns this order
            authService.validateOrderOwnership(order);

            // Business rule: verify order can be removed
            authService.validateCanRemove(order);

            // Get current user for audit
            AuthenticatedUser currentUser = authService.getCurrentUser();

            // Soft delete - mark as removed
            order.setIsRemoved(true);
            order.setRemovedAt(LocalDateTime.now());
            order.setRemovedBy(currentUser.getUserId());
            orderRepository.save(order);

            return ResponseEntity.ok(Map.of(
                        "message", "Order removed from your list",
                        "orderNumber", order.getOrderNumber(),
                        "note", "Order has been archived and hidden from your orders list"));
      }

      /**
       * Update order status (seller only).
       * Sellers can transition orders to: CONFIRMED, PROCESSING, SHIPPED.
       * Seller must have products in the order.
       */
      @PutMapping("/{orderId}/status")
      public ResponseEntity<?> updateOrderStatus(
                  @PathVariable String orderId,
                  @RequestBody StatusUpdateRequest statusRequest) {

            Order order = orderRepository.findById(orderId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

            // Authorization: verify user is a seller with products in this order
            authService.validateSellerInOrder(order);

            // Validate the new status
            OrderStatus newStatus;
            try {
                  newStatus = OrderStatus.valueOf(statusRequest.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                  throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order status");
            }

            // Validate seller-only transition
            authService.validateSellerOnlyTransition(newStatus);

            // Get current user for audit
            AuthenticatedUser currentUser = authService.getCurrentUser();

            // Update status with audit trail
            order.transitionStatus(newStatus, currentUser.getUserId(),
                        currentUser.getRole(), statusRequest.getReason());
            order.setUpdatedAt(LocalDateTime.now());
            order = orderRepository.save(order);

            return ResponseEntity.ok(Map.of(
                        "message", "Order status updated successfully",
                        "order", order));
      }

      @Data
      public static class CheckoutRequest {
            private String paymentMethod;
            private ShippingAddress shippingAddress;
            private String deliveryNotes;
      }

      @Data
      public static class CancelRequest {
            private String reason;
      }

      @Data
      public static class StatusUpdateRequest {
            private String status;
            private String reason;
      }
}
