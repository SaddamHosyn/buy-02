package ax.gritlab.buy_01.order.service;

import ax.gritlab.buy_01.order.model.*;
import ax.gritlab.buy_01.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
        private final OrderRepository orderRepository;

        /**
         * Get orders for a specific buyer.
         */
        public List<Order> getKeyOrdersForBuyer(String buyerId) {
                // Just return the latest 20 orders for simplicity
                return orderRepository
                                .findByBuyerIdAndNotRemoved(buyerId,
                                                org.springframework.data.domain.PageRequest.of(0, 20))
                                .getContent();
        }

        /**
         * Get orders for a specific seller.
         */
        public List<Order> getOrdersForSeller(String sellerId) {
                // Just return the latest 20 orders
                return orderRepository
                                .findBySellerIdInSellerIds(sellerId,
                                                org.springframework.data.domain.PageRequest.of(0, 20))
                                .getContent();
        }

        /**
         * Calculate stats for a buyer.
         */
        public BuyerStats getBuyerStats(String buyerId) {
                List<Order> orders = orderRepository.findDeliveredOrdersByBuyer(buyerId);

                double totalSpent = orders.stream().mapToDouble(Order::getTotalAmount).sum();
                int totalOrders = orders.size();

                // Calculate most bought products
                Map<String, Integer> productCounts = new HashMap<>();
                orders.stream()
                                .flatMap(o -> o.getItems().stream())
                                .forEach(item -> productCounts.merge(item.getProductName(), item.getQuantity(),
                                                (oldVal, newVal) -> oldVal + newVal));

                List<BuyerStats.ProductCount> mostBought = productCounts.entrySet().stream()
                                .map(e -> new BuyerStats.ProductCount(e.getKey(), e.getValue()))
                                .sorted((a, b) -> b.getCount() - a.getCount())
                                .limit(5)
                                .collect(Collectors.toList());

                // Calculate categories
                Map<String, Integer> categoryCounts = new HashMap<>();
                orders.stream()
                                .flatMap(o -> o.getItems().stream())
                                .filter(item -> item.getCategory() != null)
                                .forEach(item -> categoryCounts.merge(item.getCategory(), item.getQuantity(),
                                                (oldVal, newVal) -> oldVal + newVal));

                int totalUnits = categoryCounts.values().stream().mapToInt(i -> i).sum();

                List<BuyerStats.CategoryPercentage> topCategories = categoryCounts.entrySet().stream()
                                .map(e -> new BuyerStats.CategoryPercentage(e.getKey(),
                                                (e.getValue() * 100.0) / totalUnits))
                                .sorted((a, b) -> Double.compare(b.getPercentage(), a.getPercentage()))
                                .collect(Collectors.toList());

                if (topCategories.isEmpty()) {
                        topCategories = List.of(new BuyerStats.CategoryPercentage("None", 100.0));
                }

                return BuyerStats.builder()
                                .totalSpent(totalSpent)
                                .totalOrders(totalOrders)
                                .mostBoughtProducts(mostBought)
                                .topCategories(topCategories)
                                .build();
        }

        /**
         * Calculate stats for a seller.
         */
        public SellerStats getSellerStats(String sellerId) {
                List<Order> orders = orderRepository.findDeliveredOrdersBySeller(sellerId);

                // Filter items that belong to this seller
                List<OrderItem> sellerItems = orders.stream()
                                .flatMap(o -> o.getItems().stream())
                                .filter(item -> item.getSellerId().equals(sellerId))
                                .collect(Collectors.toList());

                double totalRevenue = sellerItems.stream()
                                .mapToDouble(item -> item.getPriceAtPurchase() * item.getQuantity())
                                .sum();

                int totalUnits = sellerItems.stream()
                                .mapToInt(OrderItem::getQuantity)
                                .sum();

                // Best selling products
                Map<String, Double> productRevenue = new HashMap<>();
                Map<String, Integer> productUnits = new HashMap<>();

                for (OrderItem item : sellerItems) {
                        productRevenue.merge(item.getProductName(), item.getPriceAtPurchase() * item.getQuantity(),
                                        (oldVal, newVal) -> oldVal + newVal);
                        productUnits.merge(item.getProductName(), item.getQuantity(),
                                        (oldVal, newVal) -> oldVal + newVal);
                }

                List<SellerStats.ProductRevenue> bestSelling = productRevenue.entrySet().stream()
                                .map(e -> new SellerStats.ProductRevenue(e.getKey(), e.getValue(),
                                                productUnits.get(e.getKey())))
                                .sorted((a, b) -> {
                                        int unitCompare = Integer.compare(b.getUnits(), a.getUnits());
                                        if (unitCompare != 0)
                                                return unitCompare;
                                        return Double.compare(b.getRevenue(), a.getRevenue());
                                })
                                .limit(5)
                                .collect(Collectors.toList());

                // Revenue by month (Mocked distribution for now, real implementation requires
                // aggregation)
                List<SellerStats.MonthlyRevenue> monthlyRevenue = List.of(
                                new SellerStats.MonthlyRevenue("Jan", totalRevenue * 0.1),
                                new SellerStats.MonthlyRevenue("Feb", totalRevenue * 0.15),
                                new SellerStats.MonthlyRevenue("Mar", totalRevenue * 0.2),
                                new SellerStats.MonthlyRevenue("Apr", totalRevenue * 0.25),
                                new SellerStats.MonthlyRevenue("May", totalRevenue * 0.3));

                return SellerStats.builder()
                                .totalRevenue(totalRevenue)
                                .totalUnitsSold(totalUnits)
                                .bestSellingProducts(bestSelling)
                                .revenueByMonth(monthlyRevenue)
                                .build();
        }
}
