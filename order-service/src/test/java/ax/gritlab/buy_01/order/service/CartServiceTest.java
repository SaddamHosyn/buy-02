package ax.gritlab.buy_01.order.service;

import ax.gritlab.buy_01.order.dto.request.AddToCartRequest;
import ax.gritlab.buy_01.order.dto.response.CartResponse;
import ax.gritlab.buy_01.order.exception.CartNotFoundException;
import ax.gritlab.buy_01.order.exception.CheckoutValidationException;
import ax.gritlab.buy_01.order.model.Cart;
import ax.gritlab.buy_01.order.model.CartItem;
import ax.gritlab.buy_01.order.model.CartStatus;
import ax.gritlab.buy_01.order.repository.CartRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CartService cartService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String USER_ID = "user-123";
    private static final String PRODUCT_ID = "prod-001";
    private static final String SELLER_ID = "seller-456";
    private static final String PRODUCT_NAME = "Test Widget";
    private static final double PRODUCT_PRICE = 29.99;
    private static final String PRODUCT_SERVICE_URL = "http://product-service/api/products";

    private Cart activeCart;
    private CartItem existingItem;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cartService, "productServiceUrl", PRODUCT_SERVICE_URL);

        existingItem = CartItem.builder()
                .productId(PRODUCT_ID)
                .quantity(2)
                .sellerId(SELLER_ID)
                .addedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .cachedProductName(PRODUCT_NAME)
                .cachedPrice(PRODUCT_PRICE)
                .build();

        activeCart = Cart.builder()
                .id("cart-001")
                .userId(USER_ID)
                .status(CartStatus.ACTIVE)
                .items(new ArrayList<>(List.of(existingItem)))
                .totalItems(2)
                .cachedSubtotal(59.98)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .lastAccessedAt(LocalDateTime.now())
                .build();
    }

    private ObjectNode createProductJson(int stock) {
        ObjectNode product = objectMapper.createObjectNode();
        product.put("sellerId", SELLER_ID);
        product.put("name", PRODUCT_NAME);
        product.put("price", PRODUCT_PRICE);
        product.put("stock", stock);
        return product;
    }

    private ObjectNode createProductJsonWithQuantityField(int quantity) {
        ObjectNode product = objectMapper.createObjectNode();
        product.put("sellerId", SELLER_ID);
        product.put("name", PRODUCT_NAME);
        product.put("price", PRODUCT_PRICE);
        product.put("quantity", quantity);
        return product;
    }

    // ==================== getCart Tests ====================

    @Nested
    @DisplayName("getCart")
    class GetCartTests {

        @Test
        @DisplayName("returns existing cart with refreshed prices")
        void getCart_existingCart_returnsCaerWithRefreshedPrices() {
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(activeCart));
            when(restTemplate.getForObject(anyString(), eq(JsonNode.class)))
                    .thenReturn(createProductJson(10));
            when(cartRepository.save(any(Cart.class))).thenReturn(activeCart);

            CartResponse response = cartService.getCart(USER_ID);

            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(USER_ID);
            assertThat(response.getStatus()).isEqualTo("ACTIVE");
            assertThat(response.getItems()).hasSize(1);
            verify(cartRepository).findByUserId(USER_ID);
        }

        @Test
        @DisplayName("creates new cart when none exists")
        void getCart_noExistingCart_createsNewCart() {
            Cart newCart = Cart.builder()
                    .id("cart-new")
                    .userId(USER_ID)
                    .status(CartStatus.ACTIVE)
                    .items(new ArrayList<>())
                    .totalItems(0)
                    .cachedSubtotal(0.0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(cartRepository.save(any(Cart.class))).thenReturn(newCart);

            CartResponse response = cartService.getCart(USER_ID);

            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(USER_ID);
            assertThat(response.getItems()).isEmpty();
            assertThat(response.getTotalItems()).isZero();
        }
    }

    // ==================== addItem Tests ====================

    @Nested
    @DisplayName("addItem")
    class AddItemTests {

        @Test
        @DisplayName("adds new item to cart successfully")
        void addItem_newProduct_addsToCart() {
            String newProductId = "prod-002";
            Cart emptyCart = Cart.builder()
                    .id("cart-001")
                    .userId(USER_ID)
                    .status(CartStatus.ACTIVE)
                    .items(new ArrayList<>())
                    .totalItems(0)
                    .cachedSubtotal(0.0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            ObjectNode product = objectMapper.createObjectNode();
            product.put("sellerId", SELLER_ID);
            product.put("name", "New Product");
            product.put("price", 15.0);
            product.put("stock", 50);

            AddToCartRequest request = AddToCartRequest.builder()
                    .productId(newProductId)
                    .quantity(3)
                    .build();

            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(emptyCart));
            when(restTemplate.getForObject(anyString(), eq(JsonNode.class))).thenReturn(product);
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            CartResponse response = cartService.addItem(USER_ID, request);

            assertThat(response).isNotNull();
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getProductId()).isEqualTo(newProductId);
            assertThat(response.getItems().get(0).getQuantity()).isEqualTo(3);
        }

        @Test
        @DisplayName("increments quantity when adding existing product")
        void addItem_existingProduct_incrementsQuantity() {
            AddToCartRequest request = AddToCartRequest.builder()
                    .productId(PRODUCT_ID)
                    .quantity(3)
                    .build();

            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(activeCart));
            when(restTemplate.getForObject(anyString(), eq(JsonNode.class)))
                    .thenReturn(createProductJson(100));
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            CartResponse response = cartService.addItem(USER_ID, request);

            assertThat(response).isNotNull();
            // existing qty (2) + new qty (3) = 5
            assertThat(response.getItems().get(0).getQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("throws exception when product not found")
        void addItem_productNotFound_throwsException() {
            AddToCartRequest request = AddToCartRequest.builder()
                    .productId("non-existent")
                    .quantity(1)
                    .build();

            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(activeCart));
            when(restTemplate.getForObject(anyString(), eq(JsonNode.class))).thenReturn(null);

            assertThatThrownBy(() -> cartService.addItem(USER_ID, request))
                    .isInstanceOf(CheckoutValidationException.class)
                    .hasMessageContaining("Product not found");
        }

        @Test
        @DisplayName("throws exception when requested quantity exceeds stock")
        void addItem_exceedsStock_throwsException() {
            AddToCartRequest request = AddToCartRequest.builder()
                    .productId(PRODUCT_ID)
                    .quantity(100)
                    .build();

            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(activeCart));
            when(restTemplate.getForObject(anyString(), eq(JsonNode.class)))
                    .thenReturn(createProductJson(5));

            assertThatThrownBy(() -> cartService.addItem(USER_ID, request))
                    .isInstanceOf(CheckoutValidationException.class)
                    .hasMessageContaining("Cannot add");
        }

        @Test
        @DisplayName("resets PURCHASED cart to ACTIVE when adding item")
        void addItem_purchasedCart_resetsToActive() {
            activeCart.setStatus(CartStatus.PURCHASED);
            AddToCartRequest request = AddToCartRequest.builder()
                    .productId(PRODUCT_ID)
                    .quantity(1)
                    .build();

            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(activeCart));
            when(restTemplate.getForObject(anyString(), eq(JsonNode.class)))
                    .thenReturn(createProductJson(100));
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            CartResponse response = cartService.addItem(USER_ID, request);

            assertThat(response.getStatus()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("uses quantity field when stock field absent")
        void addItem_usesQuantityField_whenStockAbsent() {
            Cart emptyCart = Cart.builder()
                    .id("cart-001")
                    .userId(USER_ID)
                    .status(CartStatus.ACTIVE)
                    .items(new ArrayList<>())
                    .totalItems(0)
                    .cachedSubtotal(0.0)
                    .build();

            AddToCartRequest request = AddToCartRequest.builder()
                    .productId(PRODUCT_ID)
                    .quantity(2)
                    .build();

            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(emptyCart));
            when(restTemplate.getForObject(anyString(), eq(JsonNode.class)))
                    .thenReturn(createProductJsonWithQuantityField(10));
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            CartResponse response = cartService.addItem(USER_ID, request);

            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("defaults to MAX_VALUE when neither stock nor quantity present")
        void addItem_noStockField_defaultsToMaxValue() {
            Cart emptyCart = Cart.builder()
                    .id("cart-001")
                    .userId(USER_ID)
                    .status(CartStatus.ACTIVE)
                    .items(new ArrayList<>())
                    .totalItems(0)
                    .cachedSubtotal(0.0)
                    .build();

            ObjectNode product = objectMapper.createObjectNode();
            product.put("sellerId", SELLER_ID);
            product.put("name", PRODUCT_NAME);
            product.put("price", PRODUCT_PRICE);
            // No stock or quantity field

            AddToCartRequest request = AddToCartRequest.builder()
                    .productId(PRODUCT_ID)
                    .quantity(999)
                    .build();

            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(emptyCart));
            when(restTemplate.getForObject(anyString(), eq(JsonNode.class))).thenReturn(product);
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            CartResponse response = cartService.addItem(USER_ID, request);

            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getQuantity()).isEqualTo(999);
        }

        @Test
        @DisplayName("creates new cart when user has no cart")
        void addItem_noCart_createsNewCart() {
            AddToCartRequest request = AddToCartRequest.builder()
                    .productId(PRODUCT_ID)
                    .quantity(1)
                    .build();

            Cart newCart = Cart.builder()
                    .id("new-cart")
                    .userId(USER_ID)
                    .status(CartStatus.ACTIVE)
                    .items(new ArrayList<>())
                    .totalItems(0)
                    .cachedSubtotal(0.0)
                    .build();

            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(cartRepository.save(any(Cart.class))).thenReturn(newCart)
                    .thenAnswer(inv -> inv.getArgument(0));
            when(restTemplate.getForObject(anyString(), eq(JsonNode.class)))
                    .thenReturn(createProductJson(10));

            CartResponse response = cartService.addItem(USER_ID, request);

            assertThat(response).isNotNull();
        }
    }

    // ==================== updateItem Tests ====================

    @Nested
    @DisplayName("updateItem")
    class UpdateItemTests {

        @Test
        @DisplayName("updates item quantity successfully")
        void updateItem_validQuantity_updatesSuccessfully() {
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(activeCart));
            when(restTemplate.getForObject(anyString(), eq(JsonNode.class)))
                    .thenReturn(createProductJson(50));
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            CartResponse response = cartService.updateItem(USER_ID, PRODUCT_ID, 5);

            assertThat(response).isNotNull();
            assertThat(response.getItems().get(0).getQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("throws when cart not found")
        void updateItem_cartNotFound_throwsException() {
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.updateItem(USER_ID, PRODUCT_ID, 5))
                    .isInstanceOf(CartNotFoundException.class)
                    .hasMessageContaining("Cart not found");
        }

        @Test
        @DisplayName("throws when product not in cart")
        void updateItem_productNotInCart_throwsException() {
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(activeCart));

            assertThatThrownBy(() -> cartService.updateItem(USER_ID, "non-existent", 5))
                    .isInstanceOf(CheckoutValidationException.class)
                    .hasMessageContaining("Product not in cart");
        }

        @Test
        @DisplayName("throws when quantity exceeds available stock")
        void updateItem_exceedsStock_throwsException() {
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(activeCart));
            when(restTemplate.getForObject(anyString(), eq(JsonNode.class)))
                    .thenReturn(createProductJson(3));

            assertThatThrownBy(() -> cartService.updateItem(USER_ID, PRODUCT_ID, 10))
                    .isInstanceOf(CheckoutValidationException.class)
                    .hasMessageContaining("Cannot set quantity");
        }

        @Test
        @DisplayName("updates quantity when product service returns null")
        void updateItem_productServiceReturnsNull_updatesAnyway() {
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(activeCart));
            when(restTemplate.getForObject(anyString(), eq(JsonNode.class))).thenReturn(null);
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            CartResponse response = cartService.updateItem(USER_ID, PRODUCT_ID, 5);

            assertThat(response.getItems().get(0).getQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("uses quantity field for stock check when stock absent")
        void updateItem_usesQuantityField_whenStockAbsent() {
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(activeCart));
            when(restTemplate.getForObject(anyString(), eq(JsonNode.class)))
                    .thenReturn(createProductJsonWithQuantityField(50));
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            CartResponse response = cartService.updateItem(USER_ID, PRODUCT_ID, 5);

            assertThat(response.getItems().get(0).getQuantity()).isEqualTo(5);
        }
    }

    // ==================== removeItem Tests ====================

    @Nested
    @DisplayName("removeItem")
    class RemoveItemTests {

        @Test
        @DisplayName("removes item from cart")
        void removeItem_existingItem_removesSuccessfully() {
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(activeCart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            CartResponse response = cartService.removeItem(USER_ID, PRODUCT_ID);

            assertThat(response.getItems()).isEmpty();
            assertThat(response.getTotalItems()).isZero();
        }

        @Test
        @DisplayName("throws when cart not found")
        void removeItem_cartNotFound_throwsException() {
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.removeItem(USER_ID, PRODUCT_ID))
                    .isInstanceOf(CartNotFoundException.class);
        }
    }

    // ==================== clearCart Tests ====================

    @Nested
    @DisplayName("clearCart")
    class ClearCartTests {

        @Test
        @DisplayName("clears all items from cart")
        void clearCart_clearsSuccessfully() {
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(activeCart));

            cartService.clearCart(USER_ID);

            assertThat(activeCart.getItems()).isEmpty();
            assertThat(activeCart.getTotalItems()).isZero();
            assertThat(activeCart.getCachedSubtotal()).isZero();
            verify(cartRepository).save(activeCart);
        }

        @Test
        @DisplayName("throws when cart not found")
        void clearCart_cartNotFound_throwsException() {
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.clearCart(USER_ID))
                    .isInstanceOf(CartNotFoundException.class);
        }
    }

    // ==================== validateCartForCheckout Tests ====================

    @Nested
    @DisplayName("validateCartForCheckout")
    class ValidateCartForCheckoutTests {

        @Test
        @DisplayName("passes validation when all items available")
        void validateCart_allAvailable_passes() {
            when(restTemplate.getForObject(anyString(), eq(JsonNode.class)))
                    .thenReturn(createProductJson(100));

            cartService.validateCartForCheckout(activeCart);

            // No exception = success
        }

        @Test
        @DisplayName("throws when cart is empty")
        void validateCart_emptyCart_throwsException() {
            Cart emptyCart = Cart.builder()
                    .items(new ArrayList<>())
                    .build();

            assertThatThrownBy(() -> cartService.validateCartForCheckout(emptyCart))
                    .isInstanceOf(CheckoutValidationException.class)
                    .hasMessage("Cart is empty");
        }

        @Test
        @DisplayName("throws when product is no longer available")
        void validateCart_productUnavailable_throwsException() {
            when(restTemplate.getForObject(anyString(), eq(JsonNode.class))).thenReturn(null);

            assertThatThrownBy(() -> cartService.validateCartForCheckout(activeCart))
                    .isInstanceOf(CheckoutValidationException.class)
                    .hasMessageContaining("no longer available");
        }

        @Test
        @DisplayName("throws when insufficient stock")
        void validateCart_insufficientStock_throwsException() {
            when(restTemplate.getForObject(anyString(), eq(JsonNode.class)))
                    .thenReturn(createProductJson(1)); // only 1 in stock, cart has 2

            assertThatThrownBy(() -> cartService.validateCartForCheckout(activeCart))
                    .isInstanceOf(CheckoutValidationException.class)
                    .hasMessageContaining("Insufficient stock");
        }
    }

    // ==================== markCartAsPurchased Tests ====================

    @Nested
    @DisplayName("markCartAsPurchased")
    class MarkCartAsPurchasedTests {

        @Test
        @DisplayName("marks cart as purchased and clears items")
        void markCartAsPurchased_success() {
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(activeCart));

            cartService.markCartAsPurchased(USER_ID);

            assertThat(activeCart.getStatus()).isEqualTo(CartStatus.PURCHASED);
            assertThat(activeCart.getItems()).isEmpty();
            assertThat(activeCart.getTotalItems()).isZero();
            assertThat(activeCart.getCachedSubtotal()).isZero();
            verify(cartRepository).save(activeCart);
        }

        @Test
        @DisplayName("throws when cart not found")
        void markCartAsPurchased_cartNotFound_throwsException() {
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.markCartAsPurchased(USER_ID))
                    .isInstanceOf(CartNotFoundException.class);
        }
    }

    // ==================== getCartEntity Tests ====================

    @Nested
    @DisplayName("getCartEntity")
    class GetCartEntityTests {

        @Test
        @DisplayName("returns cart entity when found")
        void getCartEntity_found_returnsCart() {
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(activeCart));

            Cart result = cartService.getCartEntity(USER_ID);

            assertThat(result).isEqualTo(activeCart);
        }

        @Test
        @DisplayName("throws when cart not found")
        void getCartEntity_notFound_throwsException() {
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.getCartEntity(USER_ID))
                    .isInstanceOf(CartNotFoundException.class);
        }
    }

    // ==================== Cart Response Mapping Tests ====================

    @Nested
    @DisplayName("response mapping")
    class ResponseMappingTests {

        @Test
        @DisplayName("maps cart to response with correct item details")
        void toCartResponse_mapsCorrectly() {
            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(activeCart));
            when(restTemplate.getForObject(anyString(), eq(JsonNode.class)))
                    .thenReturn(createProductJson(10));
            when(cartRepository.save(any(Cart.class))).thenReturn(activeCart);

            CartResponse response = cartService.getCart(USER_ID);

            assertThat(response.getId()).isEqualTo("cart-001");
            assertThat(response.getUserId()).isEqualTo(USER_ID);
            assertThat(response.getStatus()).isEqualTo("ACTIVE");
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getProductId()).isEqualTo(PRODUCT_ID);
            assertThat(response.getItems().get(0).getSellerId()).isEqualTo(SELLER_ID);
            assertThat(response.getItems().get(0).getCachedProductName()).isEqualTo(PRODUCT_NAME);
            assertThat(response.getItems().get(0).getCachedPrice()).isEqualTo(PRODUCT_PRICE);
        }

        @Test
        @DisplayName("calculates totals correctly for multiple items")
        void recalculateTotals_multipleItems_calculatesCorrectly() {
            CartItem item2 = CartItem.builder()
                    .productId("prod-002")
                    .quantity(3)
                    .sellerId(SELLER_ID)
                    .cachedProductName("Widget B")
                    .cachedPrice(10.0)
                    .addedAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            activeCart.getItems().add(item2);

            when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(activeCart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

            cartService.removeItem(USER_ID, "non-existent-doesnt-matter");

            // totalItems = 2 + 3 = 5
            // subtotal = (29.99 * 2) + (10.0 * 3) = 59.98 + 30.0 = 89.98
            assertThat(activeCart.getTotalItems()).isEqualTo(5);
            assertThat(activeCart.getCachedSubtotal()).isCloseTo(89.98, org.assertj.core.data.Offset.offset(0.01));
        }
    }
}
