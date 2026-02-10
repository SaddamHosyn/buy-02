package ax.gritlab.buy_01.order.service;

import ax.gritlab.buy_01.order.dto.request.CheckoutRequest;
import ax.gritlab.buy_01.order.dto.response.OrderResponse;
import ax.gritlab.buy_01.order.exception.InvalidStatusTransitionException;
import ax.gritlab.buy_01.order.exception.OrderNotFoundException;
import ax.gritlab.buy_01.order.exception.UnauthorizedException;
import ax.gritlab.buy_01.order.model.*;
import ax.gritlab.buy_01.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartService cartService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private OrderService orderService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String USER_ID = "user-123";
    private static final String USER_EMAIL = "user@example.com";
    private static final String SELLER_ID = "seller-456";
    private static final String ORDER_ID = "order-001";
    private static final String PRODUCT_ID = "prod-001";
    private static final String PRODUCT_NAME = "Test Product";
    private static final double PRODUCT_PRICE = 29.99;
    private static final String PRODUCT_SERVICE_URL = "http://product-service/api/products";

    private Cart activeCart;
    private Order existingOrder;
    private ShippingAddress shippingAddress;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "productServiceUrl", PRODUCT_SERVICE_URL);
        ReflectionTestUtils.setField(orderService, "estimatedDeliveryDays", 7);

        shippingAddress = ShippingAddress.builder()
                .fullName("John Doe")
                .addressLine1("123 Main St")
                .city("New York")
                .postalCode("10001")
                .country("USA")
                .phoneNumber("+1234567890")
                .build();

        CartItem cartItem = CartItem.builder()
                .productId(PRODUCT_ID)
                .quantity(2)
                .sellerId(SELLER_ID)
                .cachedProductName(PRODUCT_NAME)
                .cachedPrice(PRODUCT_PRICE)
                .addedAt(LocalDateTime.now())
                .build();

        activeCart = Cart.builder()
                .id("cart-001")
                .userId(USER_ID)
                .status(CartStatus.ACTIVE)
                .items(new ArrayList<>(List.of(cartItem)))
                .totalItems(2)
                .cachedSubtotal(59.98)
                .createdAt(LocalDateTime.now())
                .build();

        OrderItem orderItem = OrderItem.builder()
                .productId(PRODUCT_ID)
                .productName(PRODUCT_NAME)
                .priceAtPurchase(PRODUCT_PRICE)
                .quantity(2)
                .subtotal(59.98)
                .sellerId(SELLER_ID)
                .sellerName("Test Seller")
                .build();

        existingOrder = Order.builder()
                .id(ORDER_ID)
                .orderNumber("ORD-20260209-ABC12")
                .buyerId(USER_ID)
                .buyerName(USER_EMAIL)
                .buyerEmail(USER_EMAIL)
                .items(new ArrayList<>(List.of(orderItem)))
                .sellerIds(new HashSet<>(Set.of(SELLER_ID)))
                .subtotal(59.98)
                .shippingCost(0.0)
                .taxAmount(0.0)
                .discountAmount(0.0)
                .totalAmount(59.98)
                .paymentMethod("PAY_ON_DELIVERY")
                .paymentStatus("PAID")
                .shippingAddress(shippingAddress)
                .status(OrderStatus.CONFIRMED)
                .statusHistory(new ArrayList<>())
                .estimatedDeliveryDate(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isRemoved(false)
                .build();
    }

    private ObjectNode createProductJson() {
        ObjectNode product = objectMapper.createObjectNode();
        product.put("id", PRODUCT_ID);
        product.put("sellerId", SELLER_ID);
        product.put("name", PRODUCT_NAME);
        product.put("description", "Test description");
        product.put("price", PRODUCT_PRICE);
        product.put("stock", 10);
        ArrayNode mediaIds = product.putArray("mediaIds");
        mediaIds.add("media-001");
        return product;
    }

    @Nested
    @DisplayName("Checkout Tests")
    class CheckoutTests {

        @Test
        @DisplayName("Should successfully checkout and create order")
        void checkout_Success() {
            // Arrange
            CheckoutRequest request = CheckoutRequest.builder()
                    .shippingAddress(shippingAddress)
                    .deliveryNotes("Leave at door")
                    .paymentMethod("PAY_ON_DELIVERY")
                    .build();

            when(cartService.getCartEntity(USER_ID)).thenReturn(activeCart);
            when(restTemplate.getForObject(anyString(), eq(com.fasterxml.jackson.databind.JsonNode.class)))
                    .thenReturn(createProductJson());
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId(ORDER_ID);
                return order;
            });

            // Act
            OrderResponse response = orderService.checkout(USER_ID, USER_EMAIL, request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(ORDER_ID);
            assertThat(response.getBuyerId()).isEqualTo(USER_ID);
            assertThat(response.getBuyerEmail()).isEqualTo(USER_EMAIL);
            assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(response.getPaymentMethod()).isEqualTo("PAY_ON_DELIVERY");
            assertThat(response.getDeliveryNotes()).isEqualTo("Leave at door");
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getTotalAmount()).isEqualTo(59.98);

            verify(cartService).getCartEntity(USER_ID);
            verify(cartService).validateCartForCheckout(activeCart);
            verify(cartService).markCartAsPurchased(USER_ID);
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("Should create order with correct order item details")
        void checkout_ShouldSnapshotProductDetails() {
            // Arrange
            CheckoutRequest request = CheckoutRequest.builder()
                    .shippingAddress(shippingAddress)
                    .paymentMethod("CREDIT_CARD")
                    .build();

            when(cartService.getCartEntity(USER_ID)).thenReturn(activeCart);
            when(restTemplate.getForObject(anyString(), eq(com.fasterxml.jackson.databind.JsonNode.class)))
                    .thenReturn(createProductJson());

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            when(orderRepository.save(orderCaptor.capture())).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId(ORDER_ID);
                return order;
            });

            // Act
            orderService.checkout(USER_ID, USER_EMAIL, request);

            // Assert
            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getItems()).hasSize(1);
            OrderItem item = savedOrder.getItems().get(0);
            assertThat(item.getProductId()).isEqualTo(PRODUCT_ID);
            assertThat(item.getProductName()).isEqualTo(PRODUCT_NAME);
            assertThat(item.getPriceAtPurchase()).isEqualTo(PRODUCT_PRICE);
            assertThat(item.getQuantity()).isEqualTo(2);
            assertThat(item.getSubtotal()).isEqualTo(59.98);
            assertThat(item.getSellerId()).isEqualTo(SELLER_ID);
            assertThat(item.getThumbnailMediaId()).isEqualTo("media-001");
        }
    }

    @Nested
    @DisplayName("Get Order Tests")
    class GetOrderTests {

        @Test
        @DisplayName("Should get order by ID for buyer")
        void getOrderById_AsBuyer_Success() {
            // Arrange
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));

            // Act
            OrderResponse response = orderService.getOrderById(ORDER_ID, USER_ID);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(ORDER_ID);
            assertThat(response.getOrderNumber()).isEqualTo("ORD-20260209-ABC12");
        }

        @Test
        @DisplayName("Should get order by ID for seller")
        void getOrderById_AsSeller_Success() {
            // Arrange
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));

            // Act
            OrderResponse response = orderService.getOrderById(ORDER_ID, SELLER_ID);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(ORDER_ID);
        }

        @Test
        @DisplayName("Should throw exception when order not found")
        void getOrderById_NotFound_ThrowsException() {
            // Arrange
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.getOrderById(ORDER_ID, USER_ID))
                    .isInstanceOf(OrderNotFoundException.class)
                    .hasMessageContaining("Order not found with id: " + ORDER_ID);
        }

        @Test
        @DisplayName("Should throw exception when user is not authorized")
        void getOrderById_Unauthorized_ThrowsException() {
            // Arrange
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));

            // Act & Assert
            assertThatThrownBy(() -> orderService.getOrderById(ORDER_ID, "other-user"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("You don't have permission to view this order");
        }
    }

    @Nested
    @DisplayName("Get User Orders Tests")
    class GetUserOrdersTests {

        @Test
        @DisplayName("Should get user orders with pagination")
        void getUserOrders_Success() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> orderPage = new PageImpl<>(List.of(existingOrder), pageable, 1);
            when(orderRepository.findByBuyerIdAndNotRemoved(USER_ID, pageable)).thenReturn(orderPage);

            // Act
            Page<OrderResponse> response = orderService.getUserOrders(USER_ID, pageable);

            // Assert
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getBuyerId()).isEqualTo(USER_ID);
        }
    }

    @Nested
    @DisplayName("Get Seller Orders Tests")
    class GetSellerOrdersTests {

        @Test
        @DisplayName("Should get seller orders without status filter")
        void getSellerOrders_WithoutStatusFilter_Success() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> orderPage = new PageImpl<>(List.of(existingOrder), pageable, 1);
            when(orderRepository.findBySellerIdInSellerIds(SELLER_ID, pageable)).thenReturn(orderPage);

            // Act
            Page<OrderResponse> response = orderService.getSellerOrders(SELLER_ID, null, pageable);

            // Assert
            assertThat(response.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should get seller orders with status filter")
        void getSellerOrders_WithStatusFilter_Success() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> orderPage = new PageImpl<>(List.of(existingOrder), pageable, 1);
            when(orderRepository.findBySellerIdAndStatus(SELLER_ID, OrderStatus.CONFIRMED, pageable))
                    .thenReturn(orderPage);

            // Act
            Page<OrderResponse> response = orderService.getSellerOrders(SELLER_ID, OrderStatus.CONFIRMED, pageable);

            // Assert
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }
    }

    @Nested
    @DisplayName("Search Orders Tests")
    class SearchOrdersTests {

        @Test
        @DisplayName("Should search orders by keyword")
        void searchOrders_Success() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> orderPage = new PageImpl<>(List.of(existingOrder), pageable, 1);
            when(orderRepository.searchBySellerIdAndKeyword(SELLER_ID, "ORD-", pageable))
                    .thenReturn(orderPage);

            // Act
            Page<OrderResponse> response = orderService.searchOrders(SELLER_ID, "ORD-", pageable);

            // Assert
            assertThat(response.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Cancel Order Tests")
    class CancelOrderTests {

        @Test
        @DisplayName("Should cancel order successfully")
        void cancelOrder_Success() {
            // Arrange
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            OrderResponse response = orderService.cancelOrder(ORDER_ID, USER_ID, "Changed my mind");

            // Assert
            assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("Should throw exception when non-buyer tries to cancel")
        void cancelOrder_NotBuyer_ThrowsException() {
            // Arrange
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));

            // Act & Assert
            assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID, "other-user", "reason"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Only the buyer can cancel this order");
        }

        @Test
        @DisplayName("Should throw exception when order already shipped")
        void cancelOrder_AlreadyShipped_ThrowsException() {
            // Arrange
            existingOrder.setStatus(OrderStatus.SHIPPED);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));

            // Act & Assert
            assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID, USER_ID, "reason"))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("Cannot cancel order with status: SHIPPED");
        }

        @Test
        @DisplayName("Should throw exception when order already delivered")
        void cancelOrder_AlreadyDelivered_ThrowsException() {
            // Arrange
            existingOrder.setStatus(OrderStatus.DELIVERED);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));

            // Act & Assert
            assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID, USER_ID, "reason"))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("Cannot cancel order with status: DELIVERED");
        }

        @Test
        @DisplayName("Should throw exception when order already cancelled")
        void cancelOrder_AlreadyCancelled_ThrowsException() {
            // Arrange
            existingOrder.setStatus(OrderStatus.CANCELLED);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));

            // Act & Assert
            assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID, USER_ID, "reason"))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("Cannot cancel order with status: CANCELLED");
        }

        @Test
        @DisplayName("Should throw exception when order not found")
        void cancelOrder_NotFound_ThrowsException() {
            // Arrange
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID, USER_ID, "reason"))
                    .isInstanceOf(OrderNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Redo Order Tests")
    class RedoOrderTests {

        @Test
        @DisplayName("Should redo cancelled order successfully")
        void redoOrder_Success() {
            // Arrange
            existingOrder.setStatus(OrderStatus.CANCELLED);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));
            when(restTemplate.getForObject(anyString(), eq(com.fasterxml.jackson.databind.JsonNode.class)))
                    .thenReturn(createProductJson());
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId("new-order-id");
                return order;
            });

            // Act
            OrderResponse response = orderService.redoOrder(ORDER_ID, USER_ID);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(response.getId()).isEqualTo("new-order-id");
        }

        @Test
        @DisplayName("Should throw exception when non-buyer tries to redo")
        void redoOrder_NotBuyer_ThrowsException() {
            // Arrange
            existingOrder.setStatus(OrderStatus.CANCELLED);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));

            // Act & Assert
            assertThatThrownBy(() -> orderService.redoOrder(ORDER_ID, "other-user"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("You can only redo your own orders");
        }

        @Test
        @DisplayName("Should throw exception when order is not cancelled")
        void redoOrder_NotCancelled_ThrowsException() {
            // Arrange
            existingOrder.setStatus(OrderStatus.CONFIRMED);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));

            // Act & Assert
            assertThatThrownBy(() -> orderService.redoOrder(ORDER_ID, USER_ID))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("Can only redo cancelled orders");
        }

        @Test
        @DisplayName("Should throw exception when product no longer available")
        void redoOrder_ProductUnavailable_ThrowsException() {
            // Arrange
            existingOrder.setStatus(OrderStatus.CANCELLED);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));
            when(restTemplate.getForObject(anyString(), eq(com.fasterxml.jackson.databind.JsonNode.class)))
                    .thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> orderService.redoOrder(ORDER_ID, USER_ID))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("is no longer available");
        }

        @Test
        @DisplayName("Should throw exception when product has insufficient stock")
        void redoOrder_InsufficientStock_ThrowsException() {
            // Arrange
            existingOrder.setStatus(OrderStatus.CANCELLED);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));
            
            ObjectNode product = createProductJson();
            product.put("stock", 1); // Only 1 in stock, but order needs 2
            when(restTemplate.getForObject(anyString(), eq(com.fasterxml.jackson.databind.JsonNode.class)))
                    .thenReturn(product);

            // Act & Assert
            assertThatThrownBy(() -> orderService.redoOrder(ORDER_ID, USER_ID))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("insufficient stock");
        }

        @Test
        @DisplayName("Should throw exception when order not found")
        void redoOrder_NotFound_ThrowsException() {
            // Arrange
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.redoOrder(ORDER_ID, USER_ID))
                    .isInstanceOf(OrderNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete Order Tests")
    class DeleteOrderTests {

        @Test
        @DisplayName("Should soft delete order successfully")
        void deleteOrder_Success() {
            // Arrange
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            orderService.deleteOrder(ORDER_ID, USER_ID);

            // Assert
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());
            
            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getIsRemoved()).isTrue();
            assertThat(savedOrder.getRemovedBy()).isEqualTo(USER_ID);
            assertThat(savedOrder.getRemovedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when non-buyer tries to delete")
        void deleteOrder_NotBuyer_ThrowsException() {
            // Arrange
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));

            // Act & Assert
            assertThatThrownBy(() -> orderService.deleteOrder(ORDER_ID, "other-user"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("You can only delete your own orders");
        }

        @Test
        @DisplayName("Should throw exception when order not found")
        void deleteOrder_NotFound_ThrowsException() {
            // Arrange
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.deleteOrder(ORDER_ID, USER_ID))
                    .isInstanceOf(OrderNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Order Response Mapping Tests")
    class OrderResponseMappingTests {

        @Test
        @DisplayName("Should map all order fields correctly")
        void toOrderResponse_MapsAllFields() {
            // Arrange
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));

            // Act
            OrderResponse response = orderService.getOrderById(ORDER_ID, USER_ID);

            // Assert
            assertThat(response.getId()).isEqualTo(ORDER_ID);
            assertThat(response.getOrderNumber()).isEqualTo("ORD-20260209-ABC12");
            assertThat(response.getBuyerId()).isEqualTo(USER_ID);
            assertThat(response.getBuyerName()).isEqualTo(USER_EMAIL);
            assertThat(response.getBuyerEmail()).isEqualTo(USER_EMAIL);
            assertThat(response.getSubtotal()).isEqualTo(59.98);
            assertThat(response.getShippingCost()).isEqualTo(0.0);
            assertThat(response.getTaxAmount()).isEqualTo(0.0);
            assertThat(response.getDiscountAmount()).isEqualTo(0.0);
            assertThat(response.getTotalAmount()).isEqualTo(59.98);
            assertThat(response.getPaymentMethod()).isEqualTo("PAY_ON_DELIVERY");
            assertThat(response.getPaymentStatus()).isEqualTo("PAID");
            assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(response.getShippingAddress()).isEqualTo(shippingAddress);
        }

        @Test
        @DisplayName("Should map order items correctly")
        void toOrderResponse_MapsItemsCorrectly() {
            // Arrange
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(existingOrder));

            // Act
            OrderResponse response = orderService.getOrderById(ORDER_ID, USER_ID);

            // Assert
            assertThat(response.getItems()).hasSize(1);
            var item = response.getItems().get(0);
            assertThat(item.getProductId()).isEqualTo(PRODUCT_ID);
            assertThat(item.getProductName()).isEqualTo(PRODUCT_NAME);
            assertThat(item.getPriceAtPurchase()).isEqualTo(PRODUCT_PRICE);
            assertThat(item.getQuantity()).isEqualTo(2);
            assertThat(item.getSubtotal()).isEqualTo(59.98);
            assertThat(item.getSellerId()).isEqualTo(SELLER_ID);
        }
    }
}
