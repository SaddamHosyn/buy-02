package ax.gritlab.buy_01.order.service;

import ax.gritlab.buy_01.order.dto.request.CheckoutRequest;
import ax.gritlab.buy_01.order.dto.response.CartResponse;
import ax.gritlab.buy_01.order.dto.response.OrderItemResponse;
import ax.gritlab.buy_01.order.dto.response.OrderResponse;
import ax.gritlab.buy_01.order.exception.InvalidStatusTransitionException;
import ax.gritlab.buy_01.order.exception.OrderNotFoundException;
import ax.gritlab.buy_01.order.exception.UnauthorizedException;
import ax.gritlab.buy_01.order.model.*;
import ax.gritlab.buy_01.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final RestTemplate restTemplate;

    @Value("${product.service.url}")
    private String productServiceUrl;

    @Value("${order.estimated-delivery-days:7}")
    private int estimatedDeliveryDays;

    /**
     * Checkout - convert cart to order.
     */
    public OrderResponse checkout(String userId, String userEmail, CheckoutRequest request) {
        // Get and validate cart
        Cart cart = cartService.getCartEntity(userId);
        cartService.validateCartForCheckout(cart);

        // Create order
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setBuyerId(userId);
        order.setBuyerName(userEmail); // In real scenario, fetch from user service
        order.setBuyerEmail(userEmail);
        
        // Convert cart items to order items with snapshots
        List<OrderItem> orderItems = new ArrayList<>();
        Set<String> sellerIds = new HashSet<>();
        
        for (CartItem cartItem : cart.getItems()) {
            JsonNode product = fetchProductDetails(cartItem.getProductId());
            
            OrderItem orderItem = OrderItem.builder()
                    .productId(cartItem.getProductId())
                    .productName(product.get("name").asText())
                    .productDescription(product.has("description") ? product.get("description").asText() : "")
                    .priceAtPurchase(product.get("price").asDouble())
                    .quantity(cartItem.getQuantity())
                    .subtotal(product.get("price").asDouble() * cartItem.getQuantity())
                    .sellerId(product.get("sellerId").asText())
                    .sellerName("Seller") // Could fetch from user service
                    .thumbnailMediaId(product.has("mediaIds") && product.get("mediaIds").size() > 0 
                            ? product.get("mediaIds").get(0).asText() : null)
                    .build();
            
            orderItems.add(orderItem);
            sellerIds.add(orderItem.getSellerId());
        }
        
        order.setItems(orderItems);
        order.setSellerIds(sellerIds);
        
        // Calculate totals
        double subtotal = orderItems.stream().mapToDouble(OrderItem::getSubtotal).sum();
        order.setSubtotal(subtotal);
        order.setShippingCost(0.0);
        order.setTaxAmount(0.0);
        order.setDiscountAmount(0.0);
        order.setTotalAmount(subtotal);
        
        // Payment and shipping
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentStatus("PENDING");
        order.setShippingAddress(request.getShippingAddress());
        order.setDeliveryNotes(request.getDeliveryNotes());
        
        // Status
        order.setStatus(OrderStatus.PENDING);
        order.setStatusHistory(new ArrayList<>());
        addStatusHistoryEntry(order, null, OrderStatus.PENDING, userId, Role.CLIENT, "Order created");
        
        // Dates
        order.setEstimatedDeliveryDate(LocalDateTime.now().plusDays(estimatedDeliveryDays));
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setIsRemoved(false);
        
        // Save order
        Order savedOrder = orderRepository.save(order);
        
        // Mark cart as purchased
        cartService.markCartAsPurchased(userId);
        
        return toOrderResponse(savedOrder);
    }

    /**
     * Get order by ID.
     */
    public OrderResponse getOrderById(String orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
        
        // Check authorization
        if (!order.getBuyerId().equals(userId) && !order.getSellerIds().contains(userId)) {
            throw new UnauthorizedException("You don't have permission to view this order");
        }
        
        return toOrderResponse(order);
    }

    /**
     * Get user's orders (buyer).
     */
    public Page<OrderResponse> getUserOrders(String userId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByBuyerIdAndNotRemoved(userId, pageable);
        return orders.map(this::toOrderResponse);
    }

    /**
     * Get seller's orders.
     */
    public Page<OrderResponse> getSellerOrders(String sellerId, OrderStatus status, Pageable pageable) {
        Page<Order> orders;
        if (status != null) {
            orders = orderRepository.findBySellerIdAndStatus(sellerId, status, pageable);
        } else {
            orders = orderRepository.findBySellerIdInSellerIds(sellerId, pageable);
        }
        return orders.map(this::toOrderResponse);
    }

    /**
     * Search orders by keyword (order number or buyer email).
     */
    public Page<OrderResponse> searchOrders(String sellerId, String keyword, Pageable pageable) {
        Page<Order> orders = orderRepository.searchBySellerIdAndKeyword(sellerId, keyword, pageable);
        return orders.map(this::toOrderResponse);
    }

    /**
     * Cancel order.
     */
    public OrderResponse cancelOrder(String orderId, String userId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
        
        // Only buyer can cancel
        if (!order.getBuyerId().equals(userId)) {
            throw new UnauthorizedException("Only the buyer can cancel this order");
        }
        
        // Check if cancellable
        if (order.getStatus() == OrderStatus.SHIPPED || 
            order.getStatus() == OrderStatus.DELIVERED ||
            order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidStatusTransitionException(
                "Cannot cancel order with status: " + order.getStatus()
            );
        }
        
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        addStatusHistoryEntry(order, oldStatus, OrderStatus.CANCELLED, userId, Role.CLIENT, reason);
        
        Order saved = orderRepository.save(order);
        return toOrderResponse(saved);
    }

    /**
     * Redo order - copy items from cancelled order to cart.
     */
    public CartResponse redoOrder(String orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
        
        // Only buyer can redo their order
        if (!order.getBuyerId().equals(userId)) {
            throw new UnauthorizedException("You can only redo your own orders");
        }
        
        // Only allow redo for cancelled orders
        if (order.getStatus() != OrderStatus.CANCELLED) {
            throw new InvalidStatusTransitionException("Can only redo cancelled orders");
        }
        
        // Get user's cart
        Cart cart = cartService.getCartEntity(userId);
        
        int addedCount = 0;
        int skippedCount = 0;
        
        // Add available items to cart
        for (OrderItem orderItem : order.getItems()) {
            JsonNode product = fetchProductDetails(orderItem.getProductId());
            
            if (product != null && product.get("stock").asInt() > 0) {
                // Add to cart
                CartItem cartItem = CartItem.builder()
                        .productId(orderItem.getProductId())
                        .quantity(orderItem.getQuantity())
                        .sellerId(orderItem.getSellerId())
                        .addedAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .cachedProductName(product.get("name").asText())
                        .cachedPrice(product.get("price").asDouble())
                        .build();
                
                cart.getItems().add(cartItem);
                addedCount++;
            } else {
                skippedCount++;
            }
        }
        
        cart.setUpdatedAt(LocalDateTime.now());
        
        // Recalculate totals
        int totalItems = cart.getItems().stream().mapToInt(CartItem::getQuantity).sum();
        double subtotal = cart.getItems().stream()
                .mapToDouble(item -> item.getCachedPrice() * item.getQuantity())
                .sum();
        
        cart.setTotalItems(totalItems);
        cart.setCachedSubtotal(subtotal);
        
        // Save cart (using repository directly to avoid service method)
        Cart savedCart = cartService.getCartEntity(userId);
        savedCart.setItems(cart.getItems());
        savedCart.setTotalItems(cart.getTotalItems());
        savedCart.setCachedSubtotal(cart.getCachedSubtotal());
        savedCart.setUpdatedAt(cart.getUpdatedAt());
        
        // Build message
        String message;
        if (skippedCount == 0) {
            message = "All items added to cart";
        } else if (addedCount == 0) {
            message = "No items available from this order";
        } else {
            message = String.format("%d items added to cart. %d items no longer available.", addedCount, skippedCount);
        }
        
        return cartService.getCart(userId); // Return refreshed cart
    }

    /**
     * Soft delete order.
     */
    public void deleteOrder(String orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
        
        // Only buyer can delete
        if (!order.getBuyerId().equals(userId)) {
            throw new UnauthorizedException("You can only delete your own orders");
        }
        
        order.setIsRemoved(true);
        order.setRemovedAt(LocalDateTime.now());
        order.setRemovedBy(userId);
        order.setUpdatedAt(LocalDateTime.now());
        
        orderRepository.save(order);
    }

    // ==================== Helper Methods ====================

    private String generateOrderNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = generateRandomString(5);
        return "ORD-" + datePart + "-" + randomPart;
    }

    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            result.append(chars.charAt(random.nextInt(chars.length())));
        }
        return result.toString();
    }

    private void addStatusHistoryEntry(Order order, OrderStatus oldStatus, OrderStatus newStatus, 
                                      String userId, Role role, String reason) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .previousStatus(oldStatus)
                .newStatus(newStatus)
                .changedAt(LocalDateTime.now())
                .changedBy(userId)
                .changedByRole(role.name())
                .reason(reason)
                .build();
        order.getStatusHistory().add(history);
    }

    private JsonNode fetchProductDetails(String productId) {
        try {
            String url = productServiceUrl + "/" + productId;
            return restTemplate.getForObject(url, JsonNode.class);
        } catch (Exception e) {
            return null;
        }
    }

    private OrderResponse toOrderResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::toOrderItemResponse)
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .buyerId(order.getBuyerId())
                .buyerName(order.getBuyerName())
                .buyerEmail(order.getBuyerEmail())
                .items(items)
                .subtotal(order.getSubtotal())
                .shippingCost(order.getShippingCost())
                .taxAmount(order.getTaxAmount())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .shippingAddress(order.getShippingAddress())
                .deliveryNotes(order.getDeliveryNotes())
                .status(order.getStatus())
                .estimatedDeliveryDate(order.getEstimatedDeliveryDate())
                .actualDeliveryDate(order.getActualDeliveryDate())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderItemResponse toOrderItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .productDescription(item.getProductDescription())
                .priceAtPurchase(item.getPriceAtPurchase())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .sellerId(item.getSellerId())
                .sellerName(item.getSellerName())
                .thumbnailMediaId(item.getThumbnailMediaId())
                .build();
    }
}
