# E-Commerce Platform MVP - Testing Plan

## 📋 Overview

This testing plan focuses on the **core MVP features** for the e-commerce platform audit.

- Payment: **PAY_ON_DELIVERY only** (simplified for MVP)
- Orders: Go directly to **CONFIRMED** status
- Focus: Application functionality, not CI/CD pipeline

---

## ✅ Features to Test

| Category     | Feature                          | Priority |
| ------------ | -------------------------------- | -------- |
| **Auth**     | Register, Login, Logout          | HIGH     |
| **Products** | CRUD, Search, Filter             | HIGH     |
| **Cart**     | Add, Update, Remove, Persistence | HIGH     |
| **Checkout** | Create order (Pay on Delivery)   | HIGH     |
| **Orders**   | List, View, Cancel, Redo, Remove | HIGH     |
| **Profile**  | Buyer Stats, Seller Stats        | HIGH     |
| **UI**       | Responsive, Error Handling       | MEDIUM   |

---

# 🧪 Detailed Test Cases

## Test 1: Authentication

### 1.1 Register New Buyer (CLIENT)

| Step | Action                                   | Expected Result                       |
| ---- | ---------------------------------------- | ------------------------------------- |
| 1    | Go to `/auth/register`                   | Registration form displays            |
| 2    | Fill: Name, Email, Password, Role=CLIENT | Form validates                        |
| 3    | Click Register                           | ✅ Success message, redirect to login |
| 4    | Try same email again                     | ❌ Error: "Email already exists"      |

**API Test:**

```bash
curl -X POST http://localhost:8090/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Buyer","email":"testbuyer@test.com","password":"Password123!","role":"CLIENT"}'
# Expected: 201 Created
```

### 1.2 Register New Seller

| Step | Action                                   | Expected Result               |
| ---- | ---------------------------------------- | ----------------------------- |
| 1    | Go to `/auth/register`                   | Registration form displays    |
| 2    | Fill: Name, Email, Password, Role=SELLER | Form validates                |
| 3    | Click Register                           | ✅ Success, redirect to login |

**API Test:**

```bash
curl -X POST http://localhost:8090/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Seller","email":"testseller@test.com","password":"Password123!","role":"SELLER"}'
# Expected: 201 Created
```

### 1.3 Login

| Step | Action                     | Expected Result                 |
| ---- | -------------------------- | ------------------------------- |
| 1    | Go to `/auth/login`        | Login form displays             |
| 2    | Enter valid email/password | Form accepts                    |
| 3    | Click Login                | ✅ Redirect to home/dashboard   |
| 4    | Try wrong password         | ❌ Error: "Invalid credentials" |

**API Test:**

```bash
curl -X POST http://localhost:8090/api/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"email":"testbuyer@test.com","password":"Password123!"}'
# Expected: 200 OK with JWT token
```

### 1.4 Logout

| Step | Action                  | Expected Result                     |
| ---- | ----------------------- | ----------------------------------- |
| 1    | Click Logout button     | ✅ Token cleared, redirect to login |
| 2    | Try to access `/orders` | ❌ Redirect to login                |

---

## Test 2: Products (Search & Filter)

### 2.1 Browse Products (No Auth Required)

| Step | Action               | Expected Result                                |
| ---- | -------------------- | ---------------------------------------------- |
| 1    | Go to `/products`    | Product grid displays                          |
| 2    | Verify products load | ✅ Shows product cards with name, price, image |
| 3    | Scroll down          | ✅ Pagination works or infinite scroll         |

**API Test:**

```bash
curl -s "http://localhost:8090/api/products/search?page=0&size=12"
# Expected: 200 OK with products array
```

### 2.2 Search by Keyword

| Step | Action                      | Expected Result                                   |
| ---- | --------------------------- | ------------------------------------------------- |
| 1    | Enter "phone" in search box | Results filter                                    |
| 2    | Press Enter or click search | ✅ Only products with "phone" in name/description |
| 3    | Search "xyznonexistent"     | ✅ Empty state: "No products found"               |

**API Test:**

```bash
curl -s "http://localhost:8090/api/products/search?q=phone"
# Expected: Filtered products
```

### 2.3 Filter by Price Range

| Step | Action              | Expected Result                     |
| ---- | ------------------- | ----------------------------------- |
| 1    | Set min price = 100 | Products >= $100                    |
| 2    | Set max price = 500 | Products <= $500                    |
| 3    | Apply filters       | ✅ Only products in $100-$500 range |

**API Test:**

```bash
curl -s "http://localhost:8090/api/products/search?minPrice=100&maxPrice=500"
# Expected: Products within price range
```

### 2.4 Filter by Category

| Step | Action                        | Expected Result                    |
| ---- | ----------------------------- | ---------------------------------- |
| 1    | Select "Electronics" category | Products filter                    |
| 2    | Verify results                | ✅ Only Electronics products shown |

**API Test:**

```bash
curl -s "http://localhost:8090/api/products/search?category=Electronics"
# Expected: Electronics products only
```

### 2.5 View Product Detail

| Step | Action                       | Expected Result                                         |
| ---- | ---------------------------- | ------------------------------------------------------- |
| 1    | Click on a product card      | Navigate to `/products/{id}`                            |
| 2    | Verify details               | ✅ Name, description, price, images, stock, seller info |
| 3    | "Add to Cart" button visible | ✅ Shows for logged-in buyers                           |

---

## Test 3: Shopping Cart

### 3.1 Add Product to Cart

| Step | Action                 | Expected Result                            |
| ---- | ---------------------- | ------------------------------------------ |
| 1    | Login as BUYER         | ✅ Logged in                               |
| 2    | Go to product detail   | Product page loads                         |
| 3    | Click "Add to Cart"    | ✅ Success notification, cart icon updates |
| 4    | Add same product again | ✅ Quantity increases (not duplicate)      |

**API Test:**

```bash
curl -X POST http://localhost:8090/api/cart/items \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productId":"PRODUCT_ID","quantity":1}'
# Expected: 200 OK with cart data
```

### 3.2 View Cart

| Step | Action            | Expected Result                            |
| ---- | ----------------- | ------------------------------------------ |
| 1    | Click cart icon   | Navigate to `/cart`                        |
| 2    | Verify cart items | ✅ Product name, price, quantity, subtotal |
| 3    | Verify totals     | ✅ Cart total calculated correctly         |

**API Test:**

```bash
curl -s http://localhost:8090/api/cart \
  -H "Authorization: Bearer $TOKEN"
# Expected: Cart with items and totals
```

### 3.3 Update Quantity

| Step | Action               | Expected Result           |
| ---- | -------------------- | ------------------------- |
| 1    | Change quantity to 3 | Quantity updates          |
| 2    | Verify subtotal      | ✅ Subtotal = price × 3   |
| 3    | Set quantity to 0    | ✅ Item removed from cart |

**API Test:**

```bash
curl -X PUT http://localhost:8090/api/cart/items/PRODUCT_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"quantity":3}'
# Expected: Updated cart
```

### 3.4 Remove Item from Cart

| Step | Action                  | Expected Result                   |
| ---- | ----------------------- | --------------------------------- |
| 1    | Click remove/trash icon | Item removed                      |
| 2    | Verify cart updates     | ✅ Item gone, totals recalculated |

**API Test:**

```bash
curl -X DELETE http://localhost:8090/api/cart/items/PRODUCT_ID \
  -H "Authorization: Bearer $TOKEN"
# Expected: 200 OK with updated cart
```

### 3.5 Cart Persistence (CRITICAL)

| Step | Action                    | Expected Result           |
| ---- | ------------------------- | ------------------------- |
| 1    | Add items to cart         | Cart has items            |
| 2    | **Refresh the page (F5)** | ✅ **Cart items PERSIST** |
| 3    | Logout and login again    | ✅ Cart items still there |

**API Test:**

```bash
# Call GET /api/cart twice
curl -s http://localhost:8090/api/cart -H "Authorization: Bearer $TOKEN"
# Wait, call again
curl -s http://localhost:8090/api/cart -H "Authorization: Bearer $TOKEN"
# Expected: Same items both times
```

---

## Test 4: Checkout (Pay on Delivery)

### 4.1 Complete Checkout Flow

| Step | Action                             | Expected Result                          |
| ---- | ---------------------------------- | ---------------------------------------- |
| 1    | Go to cart with items              | Cart page shows                          |
| 2    | Click "Proceed to Checkout"        | Navigate to `/checkout`                  |
| 3    | Fill shipping address              | Form validates                           |
| 4    | Payment = "Pay on Delivery" (auto) | ✅ Only option                           |
| 5    | Click "Place Order"                | ✅ Order created, status = **CONFIRMED** |
| 6    | Verify redirect                    | ✅ Redirect to order confirmation        |
| 7    | Verify cart                        | ✅ Cart is now empty                     |

**API Test:**

```bash
curl -X POST http://localhost:8090/api/orders/checkout \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "shippingAddress":{
      "fullName":"John Doe",
      "addressLine1":"123 Main St",
      "city":"New York",
      "postalCode":"10001",
      "country":"USA"
    },
    "paymentMethod":"PAY_ON_DELIVERY"
  }'
# Expected: 200 OK with order, status=CONFIRMED
```

### 4.2 Checkout Edge Cases

| Test                 | Action                              | Expected                  |
| -------------------- | ----------------------------------- | ------------------------- |
| Empty cart checkout  | Try checkout with no items          | ❌ Error: "Cart is empty" |
| Missing address      | Omit required fields                | ❌ Validation error       |
| Product out of stock | Add product, someone buys all stock | ❌ Error on checkout      |

---

## Test 5: Orders Management (Buyer)

### 5.1 View Orders List

| Step | Action             | Expected Result                                     |
| ---- | ------------------ | --------------------------------------------------- |
| 1    | Login as BUYER     | Logged in                                           |
| 2    | Go to `/orders`    | Orders list displays                                |
| 3    | Verify order cards | ✅ Order number, date, status, total, items preview |

**API Test:**

```bash
curl -s http://localhost:8090/api/orders \
  -H "Authorization: Bearer $TOKEN"
# Expected: Paginated orders list
```

### 5.2 View Order Detail

| Step | Action            | Expected Result                                      |
| ---- | ----------------- | ---------------------------------------------------- |
| 1    | Click on an order | Navigate to `/orders/{id}`                           |
| 2    | Verify details    | ✅ Full order info, items, shipping, status timeline |

**API Test:**

```bash
curl -s http://localhost:8090/api/orders/ORDER_ID \
  -H "Authorization: Bearer $TOKEN"
# Expected: Full order details
```

### 5.3 Cancel Order

| Step | Action                 | Expected Result                    |
| ---- | ---------------------- | ---------------------------------- |
| 1    | View a CONFIRMED order | Order detail shows                 |
| 2    | Click "Cancel Order"   | Confirmation prompt                |
| 3    | Confirm cancellation   | ✅ Status changes to **CANCELLED** |
| 4    | Verify in orders list  | ✅ Shows as CANCELLED              |

**API Test:**

```bash
curl -X PATCH "http://localhost:8090/api/orders/ORDER_ID/cancel?reason=Changed%20my%20mind" \
  -H "Authorization: Bearer $TOKEN"
# Expected: 200 OK with status=CANCELLED
```

### 5.4 Redo Cancelled Order (CRITICAL)

| Step | Action                   | Expected Result                                |
| ---- | ------------------------ | ---------------------------------------------- |
| 1    | View a CANCELLED order   | Order detail shows                             |
| 2    | Click "Redo Order"       | Confirmation prompt                            |
| 3    | Confirm redo             | ✅ **NEW order created** with new order number |
| 4    | Verify new order         | ✅ Status = CONFIRMED, same items as original  |
| 5    | Original order unchanged | ✅ Still shows as CANCELLED                    |

**API Test:**

```bash
curl -X POST http://localhost:8090/api/orders/CANCELLED_ORDER_ID/redo \
  -H "Authorization: Bearer $TOKEN"
# Expected: 200 OK with NEW order (different ID, different order number)
```

### 5.5 Remove Order (Soft Delete)

| Step | Action                       | Expected Result                 |
| ---- | ---------------------------- | ------------------------------- |
| 1    | Click delete/remove on order | Confirmation prompt             |
| 2    | Confirm removal              | ✅ Order disappears from list   |
| 3    | Order is soft-deleted        | ✅ Not permanently gone from DB |

**API Test:**

```bash
curl -X DELETE http://localhost:8090/api/orders/ORDER_ID \
  -H "Authorization: Bearer $TOKEN"
# Expected: 204 No Content
```

### 5.6 Search/Filter Orders

| Step | Action                       | Expected Result          |
| ---- | ---------------------------- | ------------------------ |
| 1    | Enter order number in search | Filters results          |
| 2    | Filter by status (CONFIRMED) | ✅ Only CONFIRMED orders |
| 3    | Filter by date range         | ✅ Orders within range   |

---

## Test 6: Orders Management (Seller)

### 6.1 View Seller Orders

| Step | Action                    | Expected Result                             |
| ---- | ------------------------- | ------------------------------------------- |
| 1    | Login as SELLER           | Logged in                                   |
| 2    | Go to dashboard or orders | Seller orders display                       |
| 3    | Verify                    | ✅ Only orders containing seller's products |

**API Test:**

```bash
curl -s http://localhost:8090/api/orders/seller \
  -H "Authorization: Bearer $SELLER_TOKEN"
# Expected: Orders for this seller
```

### 6.2 Search Seller Orders

| Step | Action                 | Expected Result    |
| ---- | ---------------------- | ------------------ |
| 1    | Search by order number | ✅ Matching orders |
| 2    | Search by buyer email  | ✅ Matching orders |

**API Test:**

```bash
curl -s "http://localhost:8090/api/orders/search?q=ORD-" \
  -H "Authorization: Bearer $SELLER_TOKEN"
# Expected: Matching orders
```

---

## Test 7: Profile & Stats

### 7.1 Buyer Profile Stats

| Step | Action                     | Expected Result                               |
| ---- | -------------------------- | --------------------------------------------- |
| 1    | Login as BUYER with orders | Logged in                                     |
| 2    | Go to `/profile`           | Profile page displays                         |
| 3    | Verify stats show          | ✅ Total Spent, Total Orders, Avg Order Value |
| 4    | Verify charts              | ✅ Top products by amount (pie chart)         |
| 5    | Verify "delivered orders"  | ✅ Count of CONFIRMED + DELIVERED orders      |

**API Test:**

```bash
curl -s http://localhost:8090/api/profile/buyer/me \
  -H "Authorization: Bearer $TOKEN"
# Expected:
# {
#   "totalSpent": 299.99,
#   "totalOrders": 3,
#   "deliveredOrders": 2,
#   "topProductsByQuantity": [...],
#   "topProductsByAmount": [...],
#   "avgOrderValue": 99.99
# }
```

### 7.2 Seller Dashboard Stats

| Step | Action                     | Expected Result                          |
| ---- | -------------------------- | ---------------------------------------- |
| 1    | Login as SELLER with sales | Logged in                                |
| 2    | Go to `/seller/dashboard`  | Dashboard displays                       |
| 3    | Verify stats               | ✅ Total Earnings, Products Sold, Orders |
| 4    | Verify top products        | ✅ Best-selling products list            |

**API Test:**

```bash
curl -s http://localhost:8090/api/profile/seller/me \
  -H "Authorization: Bearer $SELLER_TOKEN"
# Expected:
# {
#   "totalEarnings": 599.99,
#   "totalProductsSold": 5,
#   "deliveredOrders": 3,
#   "topSellingProducts": [...],
#   "avgOrderValue": 119.99
# }
```

### 7.3 Stats Update After Order

| Step | Action                   | Expected Result                               |
| ---- | ------------------------ | --------------------------------------------- |
| 1    | Note current buyer stats | Record values                                 |
| 2    | Place a new order        | Order created                                 |
| 3    | Check buyer stats again  | ✅ totalSpent increased, totalOrders +1       |
| 4    | Check seller stats       | ✅ totalEarnings increased, productsSold +qty |

---

## Test 8: Seller Product Management

### 8.1 Create Product

| Step | Action                                | Expected Result                     |
| ---- | ------------------------------------- | ----------------------------------- |
| 1    | Login as SELLER                       | Logged in                           |
| 2    | Go to product form                    | Create form displays                |
| 3    | Fill: name, description, price, stock | Form validates                      |
| 4    | Click Create                          | ✅ Product created, appears in list |

**API Test:**

```bash
curl -X POST http://localhost:8090/api/products \
  -H "Authorization: Bearer $SELLER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"New Product","description":"Description","price":49.99,"stock":100}'
# Expected: 201 Created
```

### 8.2 Edit Product

| Step | Action                  | Expected Result                        |
| ---- | ----------------------- | -------------------------------------- |
| 1    | Go to product edit form | Edit form displays with current values |
| 2    | Change price            | Form updates                           |
| 3    | Save                    | ✅ Product updated                     |

### 8.3 Delete Product

| Step | Action                  | Expected Result                |
| ---- | ----------------------- | ------------------------------ |
| 1    | Click delete on product | Confirmation prompt            |
| 2    | Confirm                 | ✅ Product deleted/deactivated |

---

## Test 9: Error Handling & Edge Cases

### 9.1 Form Validation Errors

| Test                 | Action              | Expected                                    |
| -------------------- | ------------------- | ------------------------------------------- |
| Empty required field | Submit without name | ❌ "Name is required"                       |
| Invalid email        | Enter "notanemail"  | ❌ "Invalid email format"                   |
| Password too short   | Enter "123"         | ❌ "Password must be at least X characters" |
| Negative price       | Enter -10 for price | ❌ "Price must be positive"                 |

### 9.2 Authentication Errors

| Test                        | Action                   | Expected                     |
| --------------------------- | ------------------------ | ---------------------------- |
| Access orders without login | Go to `/orders`          | ❌ Redirect to login         |
| Expired token               | Wait for token to expire | ❌ Logout, redirect to login |
| Wrong password              | Login with wrong pass    | ❌ "Invalid credentials"     |

### 9.3 Authorization Errors

| Test                  | Action                           | Expected         |
| --------------------- | -------------------------------- | ---------------- |
| Buyer creates product | POST to /api/products            | ❌ 403 Forbidden |
| View other's order    | GET /api/orders/{other_id}       | ❌ 403 Forbidden |
| Cancel other's order  | PATCH /api/orders/{other}/cancel | ❌ 403 Forbidden |

### 9.4 Business Logic Errors

| Test                     | Action                 | Expected                            |
| ------------------------ | ---------------------- | ----------------------------------- |
| Redo non-cancelled order | Redo a CONFIRMED order | ❌ "Can only redo cancelled orders" |
| Cancel shipped order     | Cancel a SHIPPED order | ❌ "Cannot cancel shipped order"    |
| Checkout empty cart      | Checkout with no items | ❌ "Cart is empty"                  |
| Add out-of-stock item    | Add item with 0 stock  | ❌ "Product out of stock"           |

---

## Test 10: UI/UX Verification

### 10.1 Loading States

| Page            | Check                             |
| --------------- | --------------------------------- |
| Products list   | ✅ Spinner while loading          |
| Cart            | ✅ Spinner while loading          |
| Orders          | ✅ Spinner while loading          |
| Checkout submit | ✅ Button disabled, shows loading |

### 10.2 Empty States

| Page              | Check                           |
| ----------------- | ------------------------------- |
| Empty cart        | ✅ "Your cart is empty" message |
| No orders         | ✅ "No orders yet" message      |
| No search results | ✅ "No products found" message  |

### 10.3 Success/Error Notifications

| Action          | Check                              |
| --------------- | ---------------------------------- |
| Add to cart     | ✅ Green snackbar "Added to cart"  |
| Order placed    | ✅ Success message                 |
| Order cancelled | ✅ "Order cancelled" notification  |
| Error occurs    | ✅ Red snackbar with error message |

---

## 📝 Test Execution Checklist

### Before Testing

- [ ] All services running (ports 8761, 8081, 8082, 8083, 8084, 8090, 4200)
- [ ] MongoDB running
- [ ] Test data available (users, products)

### Authentication Tests

- [ ] Register buyer - works
- [ ] Register seller - works
- [ ] Login buyer - works
- [ ] Login seller - works
- [ ] Logout - works
- [ ] Invalid login - shows error

### Product Tests

- [ ] Product list loads
- [ ] Search by keyword works
- [ ] Price filter works
- [ ] Category filter works
- [ ] Product detail page works

### Cart Tests

- [ ] Add to cart works
- [ ] Update quantity works
- [ ] Remove item works
- [ ] **Cart persists after refresh** ⭐
- [ ] Cart totals calculate correctly

### Checkout Tests

- [ ] Checkout flow completes
- [ ] Order status = CONFIRMED (not PENDING)
- [ ] Cart empties after checkout
- [ ] Order appears in orders list

### Order Management Tests

- [ ] View orders list
- [ ] View order detail
- [ ] Cancel order works → status = CANCELLED
- [ ] **Redo order creates NEW order** ⭐
- [ ] Remove order works
- [ ] Search orders works

### Profile/Stats Tests

- [ ] Buyer stats display correctly
- [ ] Seller stats display correctly
- [ ] Stats update after new order

### Error Handling Tests

- [ ] Form validation shows errors
- [ ] Unauthorized access blocked
- [ ] API errors show messages

---

## 🚀 Quick Test Commands

```bash
# Set variables
BASE="http://localhost:8090"

# 1. Get tokens (use your existing users or create new ones)
BUYER_TOKEN="your_buyer_token_here"
SELLER_TOKEN="your_seller_token_here"

# 2. Test product search
curl -s "$BASE/api/products/search?q=&page=0&size=5" | python3 -m json.tool

# 3. Test add to cart
curl -s -X POST "$BASE/api/cart/items" \
  -H "Authorization: Bearer $BUYER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productId":"PRODUCT_ID","quantity":1}'

# 4. Test cart persistence
curl -s "$BASE/api/cart" -H "Authorization: Bearer $BUYER_TOKEN"

# 5. Test checkout
curl -s -X POST "$BASE/api/orders/checkout" \
  -H "Authorization: Bearer $BUYER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"shippingAddress":{"fullName":"Test","addressLine1":"123 St","city":"City","postalCode":"12345","country":"USA"},"paymentMethod":"PAY_ON_DELIVERY"}'

# 6. Test orders list
curl -s "$BASE/api/orders" -H "Authorization: Bearer $BUYER_TOKEN"

# 7. Test cancel order
curl -s -X PATCH "$BASE/api/orders/ORDER_ID/cancel?reason=testing" \
  -H "Authorization: Bearer $BUYER_TOKEN"

# 8. Test redo order
curl -s -X POST "$BASE/api/orders/CANCELLED_ORDER_ID/redo" \
  -H "Authorization: Bearer $BUYER_TOKEN"

# 9. Test buyer stats
curl -s "$BASE/api/profile/buyer/me" -H "Authorization: Bearer $BUYER_TOKEN"

# 10. Test seller stats
curl -s "$BASE/api/profile/seller/me" -H "Authorization: Bearer $SELLER_TOKEN"
```

---

## ⭐ Critical Tests (Must Pass)

1. **Cart persists after page refresh**
2. **Order goes to CONFIRMED status (not PENDING)**
3. **Redo creates a NEW order (not just adds to cart)**
4. **Buyer and Seller stats update correctly**
5. **Search and filter products work**
6. **Orders list works for both buyer and seller**
