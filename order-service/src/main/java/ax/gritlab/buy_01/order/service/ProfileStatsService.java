package ax.gritlab.buy_01.order.service;

import ax.gritlab.buy_01.order.dto.BuyerProfileStatsDto;
import ax.gritlab.buy_01.order.dto.ProductStatDto;
import ax.gritlab.buy_01.order.dto.SellerProfileStatsDto;
import ax.gritlab.buy_01.order.model.Order;
import ax.gritlab.buy_01.order.model.OrderItem;
import ax.gritlab.buy_01.order.model.OrderStatus;
import ax.gritlab.buy_01.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for computing profile statistics for buyers and sellers.
 */
@Service
@RequiredArgsConstructor
public class ProfileStatsService {

    private final OrderRepository orderRepository;

    private static final int TOP_PRODUCTS_LIMIT = 5;

    /**
     * Get buyer profile statistics.
     * Computes total spent, order counts, and product preferences.
     */
    public BuyerProfileStatsDto getBuyerStats(String buyerId) {
        // Get all orders for the buyer (not removed)
        List<Order> allOrders = orderRepository.findByBuyerIdAndNotRemoved(buyerId, 
                PageRequest.of(0, Integer.MAX_VALUE)).getContent();

        // Calculate order counts by status
        Map<OrderStatus, Long> ordersByStatus = allOrders.stream()
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));

        // MVP: Get completed orders (CONFIRMED or DELIVERED) for financial calculations
        List<Order> completedOrders = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.CONFIRMED)
                .collect(Collectors.toList());

        // Calculate total spent (from completed orders)
        double totalSpent = completedOrders.stream()
                .mapToDouble(Order::getTotalAmount)
                .sum();

        // Calculate average order value
        double avgOrderValue = completedOrders.isEmpty() ? 0 : totalSpent / completedOrders.size();

        // Aggregate product statistics from completed orders
        Map<String, ProductStatAggregator> productStats = new HashMap<>();
        for (Order order : completedOrders) {
            for (OrderItem item : order.getItems()) {
                productStats.computeIfAbsent(item.getProductId(), k -> new ProductStatAggregator())
                        .addItem(item);
            }
        }

        // Get top products by amount spent
        List<ProductStatDto> topByAmount = productStats.values().stream()
                .sorted(Comparator.comparing(ProductStatAggregator::getTotalAmount).reversed())
                .limit(TOP_PRODUCTS_LIMIT)
                .map(ProductStatAggregator::toDto)
                .collect(Collectors.toList());

        // Get most bought products by quantity
        List<ProductStatDto> mostBought = productStats.values().stream()
                .sorted(Comparator.comparing(ProductStatAggregator::getTotalQuantity).reversed())
                .limit(TOP_PRODUCTS_LIMIT)
                .map(ProductStatAggregator::toDto)
                .collect(Collectors.toList());

        // MVP: Count CONFIRMED as delivered for display
        int deliveredCount = ordersByStatus.getOrDefault(OrderStatus.DELIVERED, 0L).intValue() +
                             ordersByStatus.getOrDefault(OrderStatus.CONFIRMED, 0L).intValue();

        return BuyerProfileStatsDto.builder()
                .userId(buyerId)
                .totalSpent(totalSpent)
                .totalOrders(allOrders.size())
                .pendingOrders(ordersByStatus.getOrDefault(OrderStatus.PENDING, 0L).intValue() +
                               ordersByStatus.getOrDefault(OrderStatus.PROCESSING, 0L).intValue() +
                               ordersByStatus.getOrDefault(OrderStatus.SHIPPED, 0L).intValue())
                .deliveredOrders(deliveredCount)
                .cancelledOrders(ordersByStatus.getOrDefault(OrderStatus.CANCELLED, 0L).intValue())
                .topProductsByAmount(topByAmount)
                .mostBoughtProducts(mostBought)
                .averageOrderValue(avgOrderValue)
                .build();
    }

    /**
     * Get seller profile statistics.
     * Computes total earnings, sales counts, and best-selling products.
     */
    public SellerProfileStatsDto getSellerStats(String sellerId) {
        // Get all orders containing seller's products (not removed)
        List<Order> allOrders = orderRepository.findBySellerIdInSellerIds(sellerId, 
                PageRequest.of(0, Integer.MAX_VALUE)).getContent();

        // Filter to only include items belonging to this seller
        // and calculate stats per order status
        int pendingOrders = 0;
        int deliveredOrders = 0;
        int cancelledOrders = 0;
        
        double totalEarned = 0;
        int totalProductsSold = 0;
        Map<String, ProductStatAggregator> productStats = new HashMap<>();

        for (Order order : allOrders) {
            // Get only this seller's items from the order
            List<OrderItem> sellerItems = order.getItems().stream()
                    .filter(item -> sellerId.equals(item.getSellerId()))
                    .collect(Collectors.toList());

            if (sellerItems.isEmpty()) continue;

            // Count orders by status
            switch (order.getStatus()) {
                case PENDING:
                case PROCESSING:
                case SHIPPED:
                    pendingOrders++;
                    break;
                case CONFIRMED:
                    // MVP: Count confirmed orders as completed since we skip the full flow
                    deliveredOrders++;
                    for (OrderItem item : sellerItems) {
                        totalEarned += item.getSubtotal();
                        totalProductsSold += item.getQuantity();
                        productStats.computeIfAbsent(item.getProductId(), k -> new ProductStatAggregator())
                                .addItem(item);
                    }
                    break;
                case DELIVERED:
                    deliveredOrders++;
                    // Calculate earnings only from delivered orders
                    for (OrderItem item : sellerItems) {
                        totalEarned += item.getSubtotal();
                        totalProductsSold += item.getQuantity();
                        productStats.computeIfAbsent(item.getProductId(), k -> new ProductStatAggregator())
                                .addItem(item);
                    }
                    break;
                case CANCELLED:
                    cancelledOrders++;
                    break;
                default:
                    break;
            }
        }

        // Get best-selling by amount
        List<ProductStatDto> bestByAmount = productStats.values().stream()
                .sorted(Comparator.comparing(ProductStatAggregator::getTotalAmount).reversed())
                .limit(TOP_PRODUCTS_LIMIT)
                .map(ProductStatAggregator::toDto)
                .collect(Collectors.toList());

        // Get best-selling by quantity
        List<ProductStatDto> bestByQuantity = productStats.values().stream()
                .sorted(Comparator.comparing(ProductStatAggregator::getTotalQuantity).reversed())
                .limit(TOP_PRODUCTS_LIMIT)
                .map(ProductStatAggregator::toDto)
                .collect(Collectors.toList());

        // Calculate average order value (for seller's items in delivered orders)
        double avgOrderValue = deliveredOrders > 0 ? totalEarned / deliveredOrders : 0;

        return SellerProfileStatsDto.builder()
                .sellerId(sellerId)
                .totalEarned(totalEarned)
                .totalOrders(allOrders.size())
                .pendingOrders(pendingOrders)
                .deliveredOrders(deliveredOrders)
                .cancelledOrders(cancelledOrders)
                .totalProductsSold(totalProductsSold)
                .bestSellingByAmount(bestByAmount)
                .bestSellingByQuantity(bestByQuantity)
                .averageOrderValue(avgOrderValue)
                .build();
    }

    /**
     * Helper class to aggregate product statistics.
     */
    private static class ProductStatAggregator {
        private String productId;
        private String productName;
        private String thumbnailMediaId;
        private int totalQuantity = 0;
        private double totalAmount = 0;
        private int orderCount = 0;

        void addItem(OrderItem item) {
            if (productId == null) {
                this.productId = item.getProductId();
                this.productName = item.getProductName();
                this.thumbnailMediaId = item.getThumbnailMediaId();
            }
            this.totalQuantity += item.getQuantity();
            this.totalAmount += item.getSubtotal();
            this.orderCount++;
        }

        int getTotalQuantity() {
            return totalQuantity;
        }

        double getTotalAmount() {
            return totalAmount;
        }

        ProductStatDto toDto() {
            return ProductStatDto.builder()
                    .productId(productId)
                    .productName(productName)
                    .totalQuantity(totalQuantity)
                    .totalAmount(totalAmount)
                    .orderCount(orderCount)
                    .thumbnailMediaId(thumbnailMediaId)
                    .build();
        }
    }
}
