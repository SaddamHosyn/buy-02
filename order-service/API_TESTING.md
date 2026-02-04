# Order Service API Testing Guide

This document provides comprehensive API documentation and testing commands for the Order Service endpoints.

## Prerequisites

### 1. Start Required Services

```bash
# Start Service Registry (Eureka)
cd service-registry && mvn spring-boot:run &

# Wait 25 seconds for Eureka to be ready
sleep 25

# Start User Service
cd user-service && mvn spring-boot:run &

# Start Product Service
cd product-service && mvn spring-boot:run &

# Start Order Service
cd order-service && mvn spring-boot:run &

# Wait for services to start
sleep 30
```

### 2. Create Test Users

```bash
# Register a SELLER
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Seller",
    "email": "seller@test.com",
    "password": "Password123!",
    "role": "SELLER"
  }'

# Register a CLIENT
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Client",
    "email": "client@test.com",
    "password": "Password123!",
    "role": "CLIENT"
  }'
```

### 3. Get JWT Tokens

```bash
# Login as Seller
SELLER_TOKEN=$(curl -s -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"seller@test.com","password":"Password123!"}' | jq -r '.token')

# Login as Client
CLIENT_TOKEN=$(curl -s -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"client@test.com","password":"Password123!"}' | jq -r '.token')

# Save tokens for later use
echo $SELLER_TOKEN > /tmp/seller_token.txt
echo $CLIENT_TOKEN > /tmp/client_token.txt
```

### 4. Create Test Products (as Seller)

```bash
# Create Product 1 - Laptop
curl -X POST http://localhost:8082/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $SELLER_TOKEN" \
  -d '{
    "name": "Test Laptop",
    "description": "A great laptop for testing",
    "price": 999.99,
    "quantity": 10
  }'

# Create Product 2 - Phone
curl -X POST http://localhost:8082/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $SELLER_TOKEN" \
  -d '{
    "name": "Test Phone",
    "description": "A smartphone for testing",
    "price": 599.99,
    "quantity": 20
  }'

# Create Product 3 - Watch (limited stock)
curl -X POST http://localhost:8082/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $SELLER_TOKEN" \
  -d '{
    "name": "Limited Edition Watch",
    "description": "Only 2 in stock!",
    "price": 1299.99,
    "quantity": 2
  }'
```

---

## Cart Endpoints

### Base URL: `http://localhost:8084/api/cart`

All cart endpoints require authentication via JWT Bearer token.

---

### GET /api/cart

**Get Current User's Cart**

```bash
curl -X GET http://localhost:8084/api/cart \
  -H "Authorization: Bearer $CLIENT_TOKEN"
```

**Response (200 OK):**

```json
{
  "id": "6983adee722942a4be4ee268",
  "userId": "6983ad6374ebeeee12a68e30",
  "status": "ACTIVE",
  "items": [
    {
      "productId": "6983ad820492e94b8cf06c59",
      "quantity": 2,
      "sellerId": "6983ad5d74ebeeee12a68e2f",
      "addedAt": "2026-02-04T22:37:07.931",
      "updatedAt": "2026-02-04T22:37:11.449",
      "cachedProductName": "Test Laptop",
      "cachedPrice": 999.99
    }
  ],
  "totalItems": 2,
  "cachedSubtotal": 1999.98,
  "message": null
}
```

---

### POST /api/cart/items

**Add Item to Cart**

```bash
curl -X POST http://localhost:8084/api/cart/items \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "6983ad820492e94b8cf06c59",
    "quantity": 2
  }'
```

**Request Body:**

```json
{
  "productId": "string (required) - MongoDB ObjectId of the product",
  "quantity": "integer (required) - Must be >= 1"
}
```

**Response (200 OK):** Returns updated cart

**Error Responses:**

- `400` - Invalid quantity or product not found
- `403` - Not authenticated

---

### PATCH /api/cart/items/{productId}

**Update Cart Item Quantity**

```bash
curl -X PATCH http://localhost:8084/api/cart/items/6983ad820492e94b8cf06c59 \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 3
  }'
```

**Request Body:**

```json
{
  "quantity": "integer (required) - New quantity, must be >= 1"
}
```

**Response (200 OK):** Returns updated cart

---

### DELETE /api/cart/items/{productId}

**Remove Item from Cart**

```bash
curl -X DELETE http://localhost:8084/api/cart/items/6983ad820492e94b8cf06c59 \
  -H "Authorization: Bearer $CLIENT_TOKEN"
```

**Response (200 OK):** Returns updated cart

---

### DELETE /api/cart

**Clear Entire Cart**

```bash
curl -X DELETE http://localhost:8084/api/cart \
  -H "Authorization: Bearer $CLIENT_TOKEN"
```

**Response (204 No Content)**

---

## Order Endpoints

### Base URL: `http://localhost:8084/api/orders`

---

### POST /api/orders/checkout

**Create Order from Cart**

```bash
curl -X POST http://localhost:8084/api/orders/checkout \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "paymentMethod": "CREDIT_CARD",
    "shippingAddress": {
      "fullName": "Test Client",
      "addressLine1": "123 Test Street",
      "addressLine2": "Apt 4B",
      "city": "Test City",
      "state": "TS",
      "country": "Testland",
      "postalCode": "12345",
      "phone": "555-1234"
    },
    "deliveryNotes": "Leave at door"
  }'
```

**Request Body:**

```json
{
  "paymentMethod": "string (required) - CREDIT_CARD, DEBIT_CARD, PAYPAL, BANK_TRANSFER",
  "shippingAddress": {
    "fullName": "string (required)",
    "addressLine1": "string (required)",
    "addressLine2": "string (optional)",
    "city": "string (required)",
    "state": "string (optional)",
    "country": "string (required)",
    "postalCode": "string (required)",
    "phone": "string (optional)"
  },
  "deliveryNotes": "string (optional)"
}
```

**Response (201 Created):**

```json
{
  "id": "6983ae3b722942a4be4ee269",
  "orderNumber": "ORD-20260204-G5FR2",
  "buyerId": "6983ad6374ebeeee12a68e30",
  "buyerName": "client@test.com",
  "buyerEmail": "client@test.com",
  "items": [
    {
      "productId": "6983ad820492e94b8cf06c59",
      "productName": "Test Laptop",
      "productDescription": "A great laptop for testing",
      "priceAtPurchase": 999.99,
      "quantity": 1,
      "subtotal": 999.99,
      "sellerId": "6983ad5d74ebeeee12a68e2f",
      "sellerName": "Seller",
      "thumbnailMediaId": null
    }
  ],
  "subtotal": 999.99,
  "shippingCost": 0.0,
  "taxAmount": 0.0,
  "discountAmount": 0.0,
  "totalAmount": 999.99,
  "paymentMethod": "CREDIT_CARD",
  "paymentStatus": "PENDING",
  "shippingAddress": {
    "fullName": "Test Client",
    "addressLine1": "123 Test Street",
    "addressLine2": "Apt 4B",
    "city": "Test City",
    "postalCode": "12345",
    "country": "Testland",
    "phoneNumber": null,
    "formattedAddress": "Test Client\n123 Test Street\nApt 4B\nTest City, 12345\nTestland"
  },
  "deliveryNotes": "Leave at door",
  "status": "PENDING",
  "estimatedDeliveryDate": "2026-02-11T22:38:19.796",
  "actualDeliveryDate": null,
  "createdAt": "2026-02-04T22:38:19.801",
  "updatedAt": "2026-02-04T22:38:19.801"
}
```

**Error Responses:**

- `400` - Cart is empty or validation failed
- `403` - Not authenticated

---

### GET /api/orders

**Get User's Orders (Paginated)**

```bash
# Basic request
curl -X GET http://localhost:8084/api/orders \
  -H "Authorization: Bearer $CLIENT_TOKEN"

# With pagination and filtering
curl -X GET "http://localhost:8084/api/orders?page=0&size=10&status=PENDING" \
  -H "Authorization: Bearer $CLIENT_TOKEN"
```

**Query Parameters:**

- `page` - Page number (default: 0)
- `size` - Page size (default: 20)
- `status` - Filter by order status (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)

**Response (200 OK):**

```json
{
  "content": [
    {
      /* OrderResponse objects */
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": { "sorted": true, "unsorted": false, "empty": false }
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true,
  "size": 20,
  "number": 0,
  "empty": false
}
```

---

### GET /api/orders/{id}

**Get Order by ID**

```bash
curl -X GET http://localhost:8084/api/orders/6983ae3b722942a4be4ee269 \
  -H "Authorization: Bearer $CLIENT_TOKEN"
```

**Response (200 OK):** Returns full OrderResponse object

**Error Responses:**

- `404` - Order not found
- `403` - Not authorized to view this order

---

### GET /api/orders/seller

**Get Seller's Orders (Orders containing seller's products)**

```bash
curl -X GET http://localhost:8084/api/orders/seller \
  -H "Authorization: Bearer $SELLER_TOKEN"

# With pagination
curl -X GET "http://localhost:8084/api/orders/seller?page=0&size=10&status=PENDING" \
  -H "Authorization: Bearer $SELLER_TOKEN"
```

**Note:** Requires SELLER role

---

### GET /api/orders/search

**Search Orders (Seller Only)**

```bash
curl -X GET "http://localhost:8084/api/orders/search?q=laptop" \
  -H "Authorization: Bearer $SELLER_TOKEN"
```

**Query Parameters:**

- `q` - Search query (required)
- `page` - Page number (default: 0)
- `size` - Page size (default: 20)

**Note:** Requires SELLER role

---

### PATCH /api/orders/{id}/cancel

**Cancel Order**

```bash
curl -X PATCH http://localhost:8084/api/orders/6983ae3b722942a4be4ee269/cancel \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Changed my mind"
  }'
```

**Request Body (optional):**

```json
{
  "reason": "string - Cancellation reason"
}
```

**Response (200 OK):** Returns updated OrderResponse with status CANCELLED

**Error Responses:**

- `400` - Cannot cancel order with current status
- `404` - Order not found

---

### POST /api/orders/{id}/redo

**Redo Cancelled Order (Copy items back to cart)**

```bash
curl -X POST http://localhost:8084/api/orders/6983ae3b722942a4be4ee269/redo \
  -H "Authorization: Bearer $CLIENT_TOKEN"
```

**Response (200 OK):** Returns CartResponse with items from the cancelled order

**Note:** Only works on CANCELLED orders

---

### DELETE /api/orders/{id}

**Delete Order (Soft delete)**

```bash
curl -X DELETE http://localhost:8084/api/orders/6983ae3b722942a4be4ee269 \
  -H "Authorization: Bearer $CLIENT_TOKEN"
```

**Response (204 No Content)**

---

## Health & Monitoring

### GET /api/orders/health

**Health Check (No authentication required)**

```bash
curl -X GET http://localhost:8084/api/orders/health
```

**Response (200 OK):**

```json
{
  "service": "order-service",
  "status": "UP",
  "timestamp": "2026-02-04T22:41:19.896834",
  "database": "orderdb",
  "mongoStatus": "CONNECTED",
  "collections": ["carts", "wishlists", "orders"],
  "orderCount": 2,
  "cartCount": 1,
  "orderIndexes": ["_id_", "buyer_date_idx", "seller_status_date_idx", "..."],
  "cartIndexes": ["_id_", "userId", "status", "updatedAt", "..."]
}
```

---

## Error Responses

All error responses follow this format:

```json
{
  "timestamp": "2026-02-04T22:38:06.872216",
  "status": 400,
  "error": "Bad Request",
  "message": "Detailed error message"
}
```

### Validation Errors

```json
{
  "timestamp": "2026-02-04T22:41:55.736975",
  "status": 400,
  "error": "Validation Failed",
  "message": "Invalid input data",
  "fieldErrors": {
    "quantity": "Quantity must be at least 1",
    "shippingAddress": "Shipping address is required"
  }
}
```

### Common HTTP Status Codes

| Code | Meaning                        |
| ---- | ------------------------------ |
| 200  | Success                        |
| 201  | Created                        |
| 204  | No Content                     |
| 400  | Bad Request / Validation Error |
| 403  | Forbidden / Not Authenticated  |
| 404  | Not Found                      |
| 500  | Internal Server Error          |

---

## Complete Test Script

Here's a complete script to test all endpoints:

```bash
#!/bin/bash

# Configuration
BASE_URL="http://localhost:8084"
USER_SERVICE="http://localhost:8081"
PRODUCT_SERVICE="http://localhost:8082"

echo "=== Order Service API Test Suite ==="
echo ""

# 1. Register users
echo "1. Registering test users..."
curl -s -X POST $USER_SERVICE/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Seller","email":"seller@test.com","password":"Password123!","role":"SELLER"}' > /dev/null

curl -s -X POST $USER_SERVICE/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Client","email":"client@test.com","password":"Password123!","role":"CLIENT"}' > /dev/null

# 2. Get tokens
echo "2. Getting JWT tokens..."
SELLER_TOKEN=$(curl -s -X POST $USER_SERVICE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"seller@test.com","password":"Password123!"}' | jq -r '.token')

CLIENT_TOKEN=$(curl -s -X POST $USER_SERVICE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"client@test.com","password":"Password123!"}' | jq -r '.token')

# 3. Create product
echo "3. Creating test product..."
PRODUCT=$(curl -s -X POST $PRODUCT_SERVICE/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $SELLER_TOKEN" \
  -d '{"name":"Test Product","description":"For testing","price":99.99,"quantity":10}')
PRODUCT_ID=$(echo $PRODUCT | jq -r '.id')
echo "   Product ID: $PRODUCT_ID"

# 4. Test Cart - Add Item
echo "4. Testing Add to Cart..."
curl -s -X POST $BASE_URL/api/cart/items \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"productId\":\"$PRODUCT_ID\",\"quantity\":2}" | jq '.totalItems'

# 5. Test Cart - Get
echo "5. Testing Get Cart..."
curl -s $BASE_URL/api/cart \
  -H "Authorization: Bearer $CLIENT_TOKEN" | jq '.totalItems'

# 6. Test Cart - Update
echo "6. Testing Update Cart Item..."
curl -s -X PATCH $BASE_URL/api/cart/items/$PRODUCT_ID \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"quantity":1}' | jq '.totalItems'

# 7. Test Checkout
echo "7. Testing Checkout..."
ORDER=$(curl -s -X POST $BASE_URL/api/orders/checkout \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "paymentMethod":"CREDIT_CARD",
    "shippingAddress":{
      "fullName":"Test Client",
      "addressLine1":"123 Test St",
      "city":"Test City",
      "country":"Testland",
      "postalCode":"12345"
    }
  }')
ORDER_ID=$(echo $ORDER | jq -r '.id')
ORDER_NUMBER=$(echo $ORDER | jq -r '.orderNumber')
echo "   Order Number: $ORDER_NUMBER"

# 8. Test Get Orders
echo "8. Testing Get Orders..."
curl -s $BASE_URL/api/orders \
  -H "Authorization: Bearer $CLIENT_TOKEN" | jq '.totalElements'

# 9. Test Get Order by ID
echo "9. Testing Get Order by ID..."
curl -s $BASE_URL/api/orders/$ORDER_ID \
  -H "Authorization: Bearer $CLIENT_TOKEN" | jq '.orderNumber'

# 10. Test Seller Orders
echo "10. Testing Seller Orders..."
curl -s $BASE_URL/api/orders/seller \
  -H "Authorization: Bearer $SELLER_TOKEN" | jq '.totalElements'

# 11. Test Cancel Order
echo "11. Testing Cancel Order..."
curl -s -X PATCH $BASE_URL/api/orders/$ORDER_ID/cancel \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"reason":"Test cancellation"}' | jq '.status'

# 12. Test Redo Order
echo "12. Testing Redo Order..."
curl -s -X POST $BASE_URL/api/orders/$ORDER_ID/redo \
  -H "Authorization: Bearer $CLIENT_TOKEN" | jq '.totalItems'

# 13. Test Health
echo "13. Testing Health Endpoint..."
curl -s $BASE_URL/api/orders/health | jq '.status'

echo ""
echo "=== Test Suite Complete ==="
```

---

## Known Issues

1. **Cart Status After Checkout**: After checkout, the cart stays in `PURCHASED` status but still allows adding items. Consider resetting to `ACTIVE` or creating a new cart.

2. **Search Implementation**: The `/api/orders/search` endpoint may need refinement for full-text search functionality.

---

## Service Ports

| Service                   | Port      |
| ------------------------- | --------- |
| Service Registry (Eureka) | 8761      |
| API Gateway               | 8080/8443 |
| User Service              | 8081      |
| Product Service           | 8082      |
| Media Service             | 8083      |
| **Order Service**         | **8084**  |

---

## Environment Variables

```properties
# MongoDB
SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/orderdb

# Eureka
EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://localhost:8761/eureka/

# JWT (must match user-service)
jwt.secret.key=dGhpc2lzYXNlY3VyZXNlY3JldGtleWZvcnRoZWJ1eWFwcGxpY2F0aW9udGhhdGlzbG9uZ2Vub3VnaGZvckhTMjU2

# Inter-service communication
product.service.url=http://localhost:8082/products
user.service.url=http://localhost:8081/users
```
