package ax.gritlab.buy_01.order.controller;

import ax.gritlab.buy_01.order.dto.request.CheckoutRequest;
import ax.gritlab.buy_01.order.dto.response.CartResponse;
import ax.gritlab.buy_01.order.dto.response.OrderResponse;
import ax.gritlab.buy_01.order.model.OrderStatus;
import ax.gritlab.buy_01.order.model.User;
import ax.gritlab.buy_01.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Checkout - create order from cart.
     */
    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(
            @Valid @RequestBody CheckoutRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        OrderResponse order = orderService.checkout(user.getId(), user.getEmail(), request);
        return ResponseEntity.ok(order);
    }

    /**
     * Get user's orders (buyer) - paginated.
     */
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getUserOrders(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Page<OrderResponse> orders = orderService.getUserOrders(user.getId(), pageable);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get user's orders (buyer) - list endpoint for frontend compatibility.
     */
    @GetMapping("/my-orders")
    public ResponseEntity<java.util.List<OrderResponse>> getMyOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @PageableDefault(size = 100, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Page<OrderResponse> orders = orderService.getUserOrders(user.getId(), pageable);
        return ResponseEntity.ok(orders.getContent());
    }

    /**
     * Get order by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable String id,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        OrderResponse order = orderService.getOrderById(id, user.getId());
        return ResponseEntity.ok(order);
    }

    /**
     * Get seller's orders.
     */
    @GetMapping("/seller")
    @PreAuthorize("hasAuthority('SELLER')")
    public ResponseEntity<Page<OrderResponse>> getSellerOrders(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Page<OrderResponse> orders = orderService.getSellerOrders(user.getId(), status, pageable);
        return ResponseEntity.ok(orders);
    }

    /**
     * Search orders (for sellers).
     */
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('SELLER')")
    public ResponseEntity<Page<OrderResponse>> searchOrders(
            @RequestParam String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Page<OrderResponse> orders = orderService.searchOrders(user.getId(), q, pageable);
        return ResponseEntity.ok(orders);
    }

    /**
     * Cancel order.
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable String id,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        OrderResponse order = orderService.cancelOrder(id, user.getId(), reason);
        return ResponseEntity.ok(order);
    }

    /**
     * Redo order - copy items to cart.
     */
    @PostMapping("/{id}/redo")
    public ResponseEntity<CartResponse> redoOrder(
            @PathVariable String id,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        CartResponse cart = orderService.redoOrder(id, user.getId());
        return ResponseEntity.ok(cart);
    }

    /**
     * Soft delete order.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(
            @PathVariable String id,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        orderService.deleteOrder(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
