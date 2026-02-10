package ax.gritlab.buy_01.order.service;

import ax.gritlab.buy_01.order.dto.request.CheckoutRequest;
import ax.gritlab.buy_01.order.dto.response.CartResponse;
import ax.gritlab.buy_01.order.dto.response.OrderItemResponse;
import ax.gritlab.buy_01.order.dto.response.OrderResponse;
import ax.gritlab.buy_01.order.dto.StockUpdateRequest;
import ax.gritlab.buy_01.order.dto.StockUpdateResponse;
import ax.gritlab.buy_01.order.exception.InvalidStatusTransitionException;
import ax.gritlab.buy_01.order.exception.OrderNotFoundException;
import ax.gritlab.buy_01.order.exception.UnauthorizedException;
import ax.gritlab.buy_01.order.model.*;
import ax.gritlab.buy_01.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
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
        order.setPaymentStatus("PAID"); // MVP: Mark as paid directly
        order.setShippingAddress(request.getShippingAddress());
        order.setDeliveryNotes(request.getDeliveryNotes());
        
        // Status - MVP: Set directly to CONFIRMED for simplicity
        order.setStatus(OrderStatus.CONFIRMED);
        order.setStatusHistory(new ArrayList<>());
        addStatusHistoryEntry(order, null, OrderStatus.CONFIRMED, userId, Role.CLIENT, "Order placed and confirmed");
        
        // Dates
        order.setEstimatedDeliveryDate(LocalDateTime.now().plusDays(estimatedDeliveryDays));
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setIsRemoved(false);
        
        // Decrement stock for all items in the order
        decrementStockForOrder(orderItems);
        
        // Save order
        Order savedOrder = orderRepository.save(order);
        
        // Mark cart as purchased
        cartService.markCartAsPurchased(userId);
        
        return toOrderResponse(savedOrder);
    }

    /**
     * Decrement stock for all items in an order.
     * Called after checkout to update inventory.
     */
    private void decrementStockForOrder(List<OrderItem> orderItems) {
        try {
            // Build stock update request
            List<StockUpdateRequest.StockUpdateItem> items = orderItems.stream()
                    .map(item -> StockUpdateRequest.StockUpdateItem.builder()
                            .productId(item.getProductId())
                            .quantity(item.getQuantity())
                            .build())
                    .collect(Collectors.toList());
            
            StockUpdateRequest stockRequest = StockUpdateRequest.builder()
                    .items(items)
                    .build();
            
            // Call product service to decrement stock
            String url = productServiceUrl + "/internal/decrement-stock";
            log.info("Decrementing stock for {} items via: {}", items.size(), url);
            
            StockUpdateResponse response = restTemplate.postForObject(url, stockRequest, StockUpdateResponse.class);
            
            if (response != null && !response.isSuccess()) {
                log.warn("Some stock updates failed: {}", response.getMessage());
                // Log individual failures
                response.getResults().stream()
                        .filter(r -> !r.isSuccess())
                        .forEach(r -> log.warn("Failed to update stock for product {}: {}", 
                                r.getProductId(), r.getError()));
            } else {
                log.info("Stock decremented successfully for all items");
            }
        } catch (Exception e) {
            // Log error but don't fail the order - stock decrement is best effort
            // In a production system, you might want to use a saga pattern or 
            // message queue for guaranteed delivery
            log.error("Failed to decrement stock: {}. Order will still be placed.", e.getMessage());
        }
    }

    /**
     * Increment stock for all items in an order.
     * Called after order cancellation to restore inventory.
     */
    private void incrementStockForOrder(List<OrderItem> orderItems) {
        try {
            // Build stock update request
            List<StockUpdateRequest.StockUpdateItem> items = orderItems.stream()
                    .map(item -> StockUpdateRequest.StockUpdateItem.builder()
                            .productId(item.getProductId())
                            .quantity(item.getQuantity())
                            .build())
                    .collect(Collectors.toList());
            
            StockUpdateRequest stockRequest = StockUpdateRequest.builder()
                    .items(items)
                    .build();
            
            // Call product service to increment stock
            String url = productServiceUrl + "/internal/increment-stock";
            log.info("Restoring stock for {} items via: {}", items.size(), url);
            
            StockUpdateResponse response = restTemplate.postForObject(url, stockRequest, StockUpdateResponse.class);
            
            if (response != null && !response.isSuccess()) {
                log.warn("Some stock restorations failed: {}", response.getMessage());
                // Log individual failures
                response.getResults().stream()
                        .filter(r -> !r.isSuccess())
                        .forEach(r -> log.warn("Failed to restore stock for product {}: {}", 
                                r.getProductId(), r.getError()));
            } else {
                log.info("Stock restored successfully for all items");
            }
        } catch (Exception e) {
            // Log error but don't fail the cancellation
            log.error("Failed to restore stock: {}. Order cancellation will still proceed.", e.getMessage());
        }
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
        
        // Restore stock for all items in the cancelled order
        incrementStockForOrder(order.getItems());
        
        Order saved = orderRepository.save(order);
        return toOrderResponse(saved);
    }

    /**
     * Redo order - create a new order from a cancelled order.
     */
    public OrderResponse redoOrder(String orderId, String userId) {
        Order originalOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
        
        // Only buyer can redo their order
        if (!originalOrder.getBuyerId().equals(userId)) {
            throw new UnauthorizedException("You can only redo your own orders");
        }
        
        // Only allow redo for cancelled orders
        if (originalOrder.getStatus() != OrderStatus.CANCELLED) {
            throw new InvalidStatusTransitionException("Can only redo cancelled orders");
        }
        
        // Create new order
        Order newOrder = new Order();
        newOrder.setBuyerId(userId);
        newOrder.setBuyerName(originalOrder.getBuyerName());
        newOrder.setBuyerEmail(originalOrder.getBuyerEmail());
        newOrder.setOrderNumber(generateOrderNumber());
        newOrder.setOriginalOrderId(orderId); // Link to original cancelled order
        
        // Process items - update prices from current product data
        List<OrderItem> newItems = new ArrayList<>();
        Set<String> sellerIds = new HashSet<>();
        
        for (OrderItem originalItem : originalOrder.getItems()) {
            JsonNode product = fetchProductDetails(originalItem.getProductId());
            
            if (product == null || product.get("stock").asInt() < originalItem.getQuantity()) {
                throw new InvalidStatusTransitionException(
                    "Product '" + originalItem.getProductName() + "' is no longer available or has insufficient stock"
                );
            }
            
            // Create new item with current prices
            OrderItem newItem = OrderItem.builder()
                    .productId(originalItem.getProductId())
                    .productName(product.get("name").asText())
                    .sellerId(originalItem.getSellerId())
                    .sellerName(originalItem.getSellerName())
                    .quantity(originalItem.getQuantity())
                    .priceAtPurchase(product.get("price").asDouble()) // Current price
                    .subtotal(product.get("price").asDouble() * originalItem.getQuantity())
                    .thumbnailMediaId(product.has("mediaIds") && product.get("mediaIds").size() > 0 
                            ? product.get("mediaIds").get(0).asText() : null)
                    .build();
            
            newItems.add(newItem);
            sellerIds.add(newItem.getSellerId());
        }
        
        newOrder.setItems(newItems);
        newOrder.setSellerIds(sellerIds);
        
        // Calculate totals
        double subtotal = newItems.stream().mapToDouble(OrderItem::getSubtotal).sum();
        newOrder.setSubtotal(subtotal);
        newOrder.setShippingCost(0.0);
        newOrder.setTaxAmount(0.0);
        newOrder.setDiscountAmount(0.0);
        newOrder.setTotalAmount(subtotal);
        
        // Copy shipping and payment from original order
        newOrder.setPaymentMethod(originalOrder.getPaymentMethod());
        newOrder.setPaymentStatus("PAID");
        newOrder.setShippingAddress(originalOrder.getShippingAddress());
        newOrder.setDeliveryNotes(originalOrder.getDeliveryNotes());
        
        // Status - Set to CONFIRMED
        newOrder.setStatus(OrderStatus.CONFIRMED);
        newOrder.setStatusHistory(new ArrayList<>());
        addStatusHistoryEntry(newOrder, null, OrderStatus.CONFIRMED, userId, Role.CLIENT, "Order redone from cancelled order " + originalOrder.getOrderNumber());
        
        // Dates
        newOrder.setEstimatedDeliveryDate(LocalDateTime.now().plusDays(7));
        newOrder.setCreatedAt(LocalDateTime.now());
        newOrder.setUpdatedAt(LocalDateTime.now());
        newOrder.setIsRemoved(false);
        
        // Save and return
        Order savedOrder = orderRepository.save(newOrder);
        return toOrderResponse(savedOrder);
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
