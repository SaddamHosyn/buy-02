package ax.gritlab.buy_01.order.service;

import ax.gritlab.buy_01.order.dto.BuyerProfileStatsDto;
import ax.gritlab.buy_01.order.dto.SellerProfileStatsDto;
import ax.gritlab.buy_01.order.model.Order;
import ax.gritlab.buy_01.order.model.OrderItem;
import ax.gritlab.buy_01.order.model.OrderStatus;
import ax.gritlab.buy_01.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileStatsServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private ProfileStatsService profileStatsService;

    private Order deliveredOrder;
    private Order confirmedOrder;
    private Order pendingOrder;
    private Order cancelledOrder;

    @BeforeEach
    void setUp() {
        OrderItem item1 = OrderItem.builder()
                .productId("prod-1")
                .productName("Widget A")
                .priceAtPurchase(25.0)
                .quantity(2)
                .subtotal(50.0)
                .sellerId("seller-1")
                .sellerName("Seller One")
                .thumbnailMediaId("media-1")
                .build();

        OrderItem item2 = OrderItem.builder()
                .productId("prod-2")
                .productName("Widget B")
                .priceAtPurchase(10.0)
                .quantity(3)
                .subtotal(30.0)
                .sellerId("seller-1")
                .sellerName("Seller One")
                .thumbnailMediaId("media-2")
                .build();

        OrderItem item3 = OrderItem.builder()
                .productId("prod-3")
                .productName("Gadget C")
                .priceAtPurchase(100.0)
                .quantity(1)
                .subtotal(100.0)
                .sellerId("seller-2")
                .sellerName("Seller Two")
                .thumbnailMediaId("media-3")
                .build();

        deliveredOrder = Order.builder()
                .id("order-1")
                .buyerId("buyer-1")
                .status(OrderStatus.DELIVERED)
                .totalAmount(50.0)
                .items(List.of(item1))
                .build();

        confirmedOrder = Order.builder()
                .id("order-2")
                .buyerId("buyer-1")
                .status(OrderStatus.CONFIRMED)
                .totalAmount(30.0)
                .items(List.of(item2))
                .build();

        pendingOrder = Order.builder()
                .id("order-3")
                .buyerId("buyer-1")
                .status(OrderStatus.PENDING)
                .totalAmount(100.0)
                .items(List.of(item3))
                .build();

        cancelledOrder = Order.builder()
                .id("order-4")
                .buyerId("buyer-1")
                .status(OrderStatus.CANCELLED)
                .totalAmount(25.0)
                .items(List.of(item1))
                .build();
    }

    // ==================== Buyer Stats Tests ====================

    @Test
    @DisplayName("getBuyerStats returns correct totals for delivered and confirmed orders")
    void getBuyerStats_withMixedOrders_returnsCorrectTotals() {
        List<Order> orders = List.of(deliveredOrder, confirmedOrder, pendingOrder, cancelledOrder);
        when(orderRepository.findByBuyerIdAndNotRemoved(eq("buyer-1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(orders));

        BuyerProfileStatsDto stats = profileStatsService.getBuyerStats("buyer-1");

        assertThat(stats.getUserId()).isEqualTo("buyer-1");
        assertThat(stats.getTotalOrders()).isEqualTo(4);
        // Only DELIVERED + CONFIRMED count towards totalSpent
        assertThat(stats.getTotalSpent()).isEqualTo(80.0);
        // PENDING counts as pending
        assertThat(stats.getPendingOrders()).isEqualTo(1);
        // DELIVERED + CONFIRMED
        assertThat(stats.getDeliveredOrders()).isEqualTo(2);
        assertThat(stats.getCancelledOrders()).isEqualTo(1);
        // Average = 80 / 2 = 40
        assertThat(stats.getAverageOrderValue()).isEqualTo(40.0);
    }

    @Test
    @DisplayName("getBuyerStats with no orders returns zero stats")
    void getBuyerStats_withNoOrders_returnsZeroStats() {
        when(orderRepository.findByBuyerIdAndNotRemoved(eq("buyer-empty"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        BuyerProfileStatsDto stats = profileStatsService.getBuyerStats("buyer-empty");

        assertThat(stats.getUserId()).isEqualTo("buyer-empty");
        assertThat(stats.getTotalOrders()).isEqualTo(0);
        assertThat(stats.getTotalSpent()).isEqualTo(0.0);
        assertThat(stats.getPendingOrders()).isEqualTo(0);
        assertThat(stats.getDeliveredOrders()).isEqualTo(0);
        assertThat(stats.getCancelledOrders()).isEqualTo(0);
        assertThat(stats.getAverageOrderValue()).isEqualTo(0.0);
        assertThat(stats.getTopProductsByAmount()).isEmpty();
        assertThat(stats.getMostBoughtProducts()).isEmpty();
    }

    @Test
    @DisplayName("getBuyerStats computes product statistics correctly")
    void getBuyerStats_computesProductStats() {
        List<Order> orders = List.of(deliveredOrder, confirmedOrder);
        when(orderRepository.findByBuyerIdAndNotRemoved(eq("buyer-1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(orders));

        BuyerProfileStatsDto stats = profileStatsService.getBuyerStats("buyer-1");

        assertThat(stats.getTopProductsByAmount()).hasSize(2);
        assertThat(stats.getMostBoughtProducts()).hasSize(2);
        // prod-1 has 50.0 total, prod-2 has 30.0 total - top by amount should be prod-1 first
        assertThat(stats.getTopProductsByAmount().get(0).getProductId()).isEqualTo("prod-1");
        // prod-2 has quantity 3, prod-1 has quantity 2 - most bought should be prod-2 first
        assertThat(stats.getMostBoughtProducts().get(0).getProductId()).isEqualTo("prod-2");
    }

    @Test
    @DisplayName("getBuyerStats counts PROCESSING and SHIPPED as pending")
    void getBuyerStats_countsProcessingAndShippedAsPending() {
        Order processingOrder = Order.builder()
                .id("order-p")
                .buyerId("buyer-1")
                .status(OrderStatus.PROCESSING)
                .totalAmount(10.0)
                .items(Collections.emptyList())
                .build();

        Order shippedOrder = Order.builder()
                .id("order-s")
                .buyerId("buyer-1")
                .status(OrderStatus.SHIPPED)
                .totalAmount(20.0)
                .items(Collections.emptyList())
                .build();

        when(orderRepository.findByBuyerIdAndNotRemoved(eq("buyer-1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pendingOrder, processingOrder, shippedOrder)));

        BuyerProfileStatsDto stats = profileStatsService.getBuyerStats("buyer-1");

        // PENDING + PROCESSING + SHIPPED = 3 pending
        assertThat(stats.getPendingOrders()).isEqualTo(3);
        assertThat(stats.getDeliveredOrders()).isEqualTo(0);
    }

    // ==================== Seller Stats Tests ====================

    @Test
    @DisplayName("getSellerStats returns correct totals for seller items")
    void getSellerStats_withMixedOrders_returnsCorrectTotals() {
        List<Order> orders = List.of(deliveredOrder, confirmedOrder, pendingOrder, cancelledOrder);
        when(orderRepository.findBySellerIdInSellerIds(eq("seller-1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(orders));

        SellerProfileStatsDto stats = profileStatsService.getSellerStats("seller-1");

        assertThat(stats.getSellerId()).isEqualTo("seller-1");
        assertThat(stats.getTotalOrders()).isEqualTo(4);
        // DELIVERED item1: 50.0 + CONFIRMED item2: 30.0 = 80.0
        assertThat(stats.getTotalEarned()).isEqualTo(80.0);
        // Total products sold: 2 + 3 = 5
        assertThat(stats.getTotalProductsSold()).isEqualTo(5);
        // pendingOrder has seller-2 items, so it's skipped for seller-1
        assertThat(stats.getPendingOrders()).isEqualTo(0);
        // DELIVERED + CONFIRMED
        assertThat(stats.getDeliveredOrders()).isEqualTo(2);
        assertThat(stats.getCancelledOrders()).isEqualTo(1);
        // Average = 80 / 2 = 40
        assertThat(stats.getAverageOrderValue()).isEqualTo(40.0);
    }

    @Test
    @DisplayName("getSellerStats with no orders returns zero stats")
    void getSellerStats_withNoOrders_returnsZeroStats() {
        when(orderRepository.findBySellerIdInSellerIds(eq("seller-empty"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        SellerProfileStatsDto stats = profileStatsService.getSellerStats("seller-empty");

        assertThat(stats.getSellerId()).isEqualTo("seller-empty");
        assertThat(stats.getTotalOrders()).isEqualTo(0);
        assertThat(stats.getTotalEarned()).isEqualTo(0.0);
        assertThat(stats.getTotalProductsSold()).isEqualTo(0);
        assertThat(stats.getPendingOrders()).isEqualTo(0);
        assertThat(stats.getDeliveredOrders()).isEqualTo(0);
        assertThat(stats.getCancelledOrders()).isEqualTo(0);
        assertThat(stats.getAverageOrderValue()).isEqualTo(0.0);
        assertThat(stats.getBestSellingByAmount()).isEmpty();
        assertThat(stats.getBestSellingByQuantity()).isEmpty();
    }

    @Test
    @DisplayName("getSellerStats filters items to only seller's items")
    void getSellerStats_filtersToSellerItems() {
        // Order has items from both seller-1 and seller-2
        OrderItem sellerOneItem = OrderItem.builder()
                .productId("prod-1")
                .productName("Widget A")
                .priceAtPurchase(25.0)
                .quantity(2)
                .subtotal(50.0)
                .sellerId("seller-1")
                .build();

        OrderItem sellerTwoItem = OrderItem.builder()
                .productId("prod-3")
                .productName("Gadget C")
                .priceAtPurchase(100.0)
                .quantity(1)
                .subtotal(100.0)
                .sellerId("seller-2")
                .build();

        Order mixedOrder = Order.builder()
                .id("order-mixed")
                .status(OrderStatus.DELIVERED)
                .totalAmount(150.0)
                .items(List.of(sellerOneItem, sellerTwoItem))
                .build();

        when(orderRepository.findBySellerIdInSellerIds(eq("seller-1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mixedOrder)));

        SellerProfileStatsDto stats = profileStatsService.getSellerStats("seller-1");

        // Only seller-1's item should count
        assertThat(stats.getTotalEarned()).isEqualTo(50.0);
        assertThat(stats.getTotalProductsSold()).isEqualTo(2);
        assertThat(stats.getBestSellingByAmount()).hasSize(1);
        assertThat(stats.getBestSellingByAmount().get(0).getProductId()).isEqualTo("prod-1");
    }

    @Test
    @DisplayName("getSellerStats skips orders where seller has no items")
    void getSellerStats_skipsOrdersWithNoSellerItems() {
        // An order where all items belong to a different seller
        OrderItem otherSellerItem = OrderItem.builder()
                .productId("prod-other")
                .productName("Other Product")
                .priceAtPurchase(50.0)
                .quantity(1)
                .subtotal(50.0)
                .sellerId("seller-other")
                .build();

        Order otherSellerOrder = Order.builder()
                .id("order-other")
                .status(OrderStatus.DELIVERED)
                .totalAmount(50.0)
                .items(List.of(otherSellerItem))
                .build();

        when(orderRepository.findBySellerIdInSellerIds(eq("seller-1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(otherSellerOrder)));

        SellerProfileStatsDto stats = profileStatsService.getSellerStats("seller-1");

        assertThat(stats.getTotalEarned()).isEqualTo(0.0);
        assertThat(stats.getTotalProductsSold()).isEqualTo(0);
        assertThat(stats.getDeliveredOrders()).isEqualTo(0);
        assertThat(stats.getPendingOrders()).isEqualTo(0);
    }

    @Test
    @DisplayName("getSellerStats counts PENDING, PROCESSING, SHIPPED as pending")
    void getSellerStats_countsPendingStatuses() {
        OrderItem sellerItem = OrderItem.builder()
                .productId("prod-1")
                .productName("Widget")
                .priceAtPurchase(10.0)
                .quantity(1)
                .subtotal(10.0)
                .sellerId("seller-1")
                .build();

        Order processing = Order.builder()
                .id("o-proc")
                .status(OrderStatus.PROCESSING)
                .totalAmount(10.0)
                .items(List.of(sellerItem))
                .build();

        Order shipped = Order.builder()
                .id("o-ship")
                .status(OrderStatus.SHIPPED)
                .totalAmount(10.0)
                .items(List.of(sellerItem))
                .build();

        Order pending = Order.builder()
                .id("o-pend")
                .status(OrderStatus.PENDING)
                .totalAmount(10.0)
                .items(List.of(sellerItem))
                .build();

        when(orderRepository.findBySellerIdInSellerIds(eq("seller-1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(processing, shipped, pending)));

        SellerProfileStatsDto stats = profileStatsService.getSellerStats("seller-1");

        assertThat(stats.getPendingOrders()).isEqualTo(3);
        assertThat(stats.getDeliveredOrders()).isEqualTo(0);
        assertThat(stats.getTotalEarned()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getSellerStats handles CONFIRMED same as DELIVERED (merged case)")
    void getSellerStats_confirmedAndDeliveredMerged() {
        OrderItem sellerItem = OrderItem.builder()
                .productId("prod-1")
                .productName("Widget")
                .priceAtPurchase(20.0)
                .quantity(1)
                .subtotal(20.0)
                .sellerId("seller-1")
                .build();

        Order confirmed = Order.builder()
                .id("o-conf")
                .status(OrderStatus.CONFIRMED)
                .totalAmount(20.0)
                .items(List.of(sellerItem))
                .build();

        Order delivered = Order.builder()
                .id("o-del")
                .status(OrderStatus.DELIVERED)
                .totalAmount(20.0)
                .items(List.of(sellerItem))
                .build();

        when(orderRepository.findBySellerIdInSellerIds(eq("seller-1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(confirmed, delivered)));

        SellerProfileStatsDto stats = profileStatsService.getSellerStats("seller-1");

        assertThat(stats.getDeliveredOrders()).isEqualTo(2);
        assertThat(stats.getTotalEarned()).isEqualTo(40.0);
        assertThat(stats.getTotalProductsSold()).isEqualTo(2);
    }
}
