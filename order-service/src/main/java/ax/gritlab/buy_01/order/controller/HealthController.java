package ax.gritlab.buy_01.order.controller;

import ax.gritlab.buy_01.order.model.*;
import ax.gritlab.buy_01.order.repository.CartRepository;
import ax.gritlab.buy_01.order.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Health and verification endpoints for database testing.
 * 
 * USE THESE ENDPOINTS TO VERIFY YOUR DATABASE:
 * 
 * 1. GET /api/orders/health - Check if service and MongoDB are connected
 * 2. POST /api/orders/verify/create-test-data - Create sample data
 * 3. GET /api/orders/verify/test-queries - Run test queries
 * 4. DELETE /api/orders/verify/cleanup - Remove test data
 */
@RestController
@RequestMapping("/api/orders")
public class HealthController {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    private static final String TEST_BUYER_ID = "test-buyer-verification";
    private static final String TEST_SELLER_ID = "test-seller-verification";

    /**
     * Health check endpoint - verifies MongoDB connection.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", "order-service");
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now().toString());

        try {
            // Test MongoDB connection
            String dbName = mongoTemplate.getDb().getName();
            Set<String> collections = mongoTemplate.getCollectionNames();
            
            response.put("database", dbName);
            response.put("mongoStatus", "CONNECTED");
            response.put("collections", collections);
            response.put("orderCount", orderRepository.count());
            response.put("cartCount", cartRepository.count());

            // Check indexes
            var orderIndexes = mongoTemplate.getCollection("orders").listIndexes();
            var cartIndexes = mongoTemplate.getCollection("carts").listIndexes();
            
            List<String> orderIndexNames = new ArrayList<>();
            List<String> cartIndexNames = new ArrayList<>();
            orderIndexes.forEach(doc -> orderIndexNames.add(doc.getString("name")));
            cartIndexes.forEach(doc -> cartIndexNames.add(doc.getString("name")));
            
            response.put("orderIndexes", orderIndexNames);
            response.put("cartIndexes", cartIndexNames);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("mongoStatus", "ERROR");
            response.put("error", e.getMessage());
            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * Create test data to verify database operations.
     */
    @PostMapping("/verify/create-test-data")
    public ResponseEntity<Map<String, Object>> createTestData() {
        Map<String, Object> response = new LinkedHashMap<>();

        try {
            // Create test order
            OrderItem item = OrderItem.builder()
                    .productId("test-product-001")
                    .productName("Test Product")
                    .productDescription("A test product for verification")
                    .priceAtPurchase(99.99)
                    .quantity(2)
                    .sellerId(TEST_SELLER_ID)
                    .sellerName("Test Seller")
                    .build();
            item.calculateSubtotal();

            ShippingAddress address = ShippingAddress.builder()
                    .fullName("Test User")
                    .addressLine1("123 Test Street")
                    .city("Test City")
                    .postalCode("12345")
                    .country("Test Country")
                    .build();

            Order order = Order.builder()
                    .orderNumber(Order.generateOrderNumber())
                    .buyerId(TEST_BUYER_ID)
                    .buyerName("Test Buyer")
                    .buyerEmail("test@verification.com")
                    .items(List.of(item))
                    .shippingAddress(address)
                    .paymentMethod("PAY_ON_DELIVERY")
                    .status(OrderStatus.PENDING)
                    .statusHistory(new ArrayList<>())
                    .build();
            
            order.calculateTotals();
            order.getStatusHistory().add(OrderStatusHistory.createInitial(TEST_BUYER_ID, "CLIENT"));

            Order savedOrder = orderRepository.save(order);

            // Create test cart
            Cart cart = Cart.createForUser(TEST_BUYER_ID);
            CartItem cartItem = CartItem.create("test-product-002", 3, TEST_SELLER_ID);
            cartItem.setCachedProductName("Test Cart Product");
            cartItem.setCachedPrice(49.99);
            cart.addItem(cartItem);

            Cart savedCart = cartRepository.save(cart);

            response.put("success", true);
            response.put("message", "Test data created successfully");
            response.put("order", Map.of(
                    "id", savedOrder.getId(),
                    "orderNumber", savedOrder.getOrderNumber(),
                    "totalAmount", savedOrder.getTotalAmount(),
                    "status", savedOrder.getStatus(),
                    "sellerIds", savedOrder.getSellerIds()
            ));
            response.put("cart", Map.of(
                    "id", savedCart.getId(),
                    "userId", savedCart.getUserId(),
                    "totalItems", savedCart.getTotalItems(),
                    "subtotal", savedCart.getCachedSubtotal()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Run test queries to verify database operations.
     */
    @GetMapping("/verify/test-queries")
    public ResponseEntity<Map<String, Object>> testQueries() {
        Map<String, Object> response = new LinkedHashMap<>();
        List<Map<String, Object>> testResults = new ArrayList<>();

        // Test 1: Find orders by buyer
        try {
            var orders = orderRepository.findByBuyerIdAndNotRemoved(
                    TEST_BUYER_ID, 
                    org.springframework.data.domain.PageRequest.of(0, 10)
            );
            testResults.add(Map.of(
                    "test", "Find orders by buyer ID",
                    "passed", !orders.isEmpty(),
                    "result", orders.getTotalElements() + " orders found"
            ));
        } catch (Exception e) {
            testResults.add(Map.of("test", "Find orders by buyer ID", "passed", false, "error", e.getMessage()));
        }

        // Test 2: Find orders by seller
        try {
            var orders = orderRepository.findBySellerIdInSellerIds(
                    TEST_SELLER_ID,
                    org.springframework.data.domain.PageRequest.of(0, 10)
            );
            testResults.add(Map.of(
                    "test", "Find orders by seller ID (denormalized)",
                    "passed", !orders.isEmpty(),
                    "result", orders.getTotalElements() + " orders found"
            ));
        } catch (Exception e) {
            testResults.add(Map.of("test", "Find orders by seller ID", "passed", false, "error", e.getMessage()));
        }

        // Test 3: Find cart by user
        try {
            var cart = cartRepository.findByUserId(TEST_BUYER_ID);
            testResults.add(Map.of(
                    "test", "Find cart by user ID",
                    "passed", cart.isPresent(),
                    "result", cart.isPresent() ? cart.get().getTotalItems() + " items in cart" : "No cart found"
            ));
        } catch (Exception e) {
            testResults.add(Map.of("test", "Find cart by user ID", "passed", false, "error", e.getMessage()));
        }

        // Test 4: Count by status
        try {
            long pendingCount = orderRepository.countByStatus(OrderStatus.PENDING);
            testResults.add(Map.of(
                    "test", "Count orders by status",
                    "passed", true,
                    "result", pendingCount + " PENDING orders"
            ));
        } catch (Exception e) {
            testResults.add(Map.of("test", "Count orders by status", "passed", false, "error", e.getMessage()));
        }

        boolean allPassed = testResults.stream().allMatch(t -> (Boolean) t.get("passed"));
        
        response.put("allTestsPassed", allPassed);
        response.put("tests", testResults);
        response.put("summary", allPassed 
                ? "✅ All database queries working correctly!" 
                : "❌ Some tests failed - check results above");

        return ResponseEntity.ok(response);
    }

    /**
     * Clean up test data.
     */
    @DeleteMapping("/verify/cleanup")
    public ResponseEntity<Map<String, Object>> cleanup() {
        Map<String, Object> response = new LinkedHashMap<>();

        try {
            // Delete test orders
            var orders = orderRepository.findByBuyerIdAndNotRemoved(
                    TEST_BUYER_ID,
                    org.springframework.data.domain.PageRequest.of(0, 100)
            );
            orders.forEach(order -> orderRepository.delete(order));

            // Delete test cart
            cartRepository.deleteByUserId(TEST_BUYER_ID);

            response.put("success", true);
            response.put("message", "Test data cleaned up");
            response.put("ordersDeleted", orders.getTotalElements());
            response.put("cartsDeleted", 1);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
