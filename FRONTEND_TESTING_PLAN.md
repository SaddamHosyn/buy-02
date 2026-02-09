# Frontend Testing Plan - E-Commerce Platform MVP

## 📋 Overview

This plan tests the **Angular frontend** and its integration with the backend APIs.

| Environment | URL |
|-------------|-----|
| Frontend | http://localhost:4200 |
| Backend Gateway | http://localhost:8090 |

### Test Credentials
| Role | Email | Password |
|------|-------|----------|
| **Buyer (CLIENT)** | client@test.com | password123 |
| **Seller** | seller@test.com | password123 |

---

## 🔍 Pre-Test Checklist

Before starting, verify:
- [ ] All services are running (Eureka, User, Product, Media, Order, Gateway)
- [ ] Frontend is running on port 4200
- [ ] MongoDB has test data seeded
- [ ] Browser console is open for error monitoring

---

# 🧪 Test Categories

| # | Category | Tests | Priority |
|---|----------|-------|----------|
| 1 | App Loading & Navigation | 5 | HIGH |
| 2 | Authentication UI | 6 | HIGH |
| 3 | Product Browsing | 6 | HIGH |
| 4 | Shopping Cart UI | 7 | HIGH |
| 5 | Checkout Flow | 4 | HIGH |
| 6 | Order Management UI | 6 | HIGH |
| 7 | Profile & Stats | 4 | HIGH |
| 8 | Seller Dashboard | 5 | HIGH |
| 9 | Responsive Design | 3 | MEDIUM |
| 10 | Error Handling | 4 | MEDIUM |

**Total: 50 tests**

---

# 📝 Detailed Test Cases

## Test Category 1: App Loading & Navigation

### 1.1 Initial Page Load
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open http://localhost:4200 | App loads without errors |
| 2 | Check browser console | No JavaScript errors |
| 3 | Verify redirect | Redirects to /products |

### 1.2 Navigation Bar Display (Not Logged In)
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View navbar | Shows: Logo, Products link, Login/Register buttons |
| 2 | Verify no cart icon | Cart icon should be hidden for guests |
| 3 | Verify no profile menu | Profile dropdown hidden |

### 1.3 Navigation Bar Display (Logged In as Buyer)
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Login as client@test.com | Login successful |
| 2 | View navbar | Shows: Logo, Products, Cart icon, Profile dropdown |
| 3 | Cart icon | Shows badge with item count (if any) |
| 4 | Profile dropdown | Shows: My Orders, Profile, Logout |

### 1.4 Navigation Bar Display (Logged In as Seller)
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Login as seller@test.com | Login successful |
| 2 | View navbar | Shows: Seller Dashboard link in menu |
| 3 | Verify dashboard access | Can click to access /seller/dashboard |

### 1.5 Protected Route Guards
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | While logged out, go to /cart | Redirects to /auth/login |
| 2 | While logged out, go to /orders | Redirects to /auth/login |
| 3 | As buyer, go to /seller/dashboard | Redirects away (forbidden) |

---

## Test Category 2: Authentication UI

### 2.1 Login Form Display
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Go to /auth/login | Login form displays |
| 2 | Verify form fields | Email input, Password input, Submit button |
| 3 | Verify link to register | "Don't have an account? Register" link present |

### 2.2 Login - Valid Credentials
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Enter: client@test.com / password123 | Form accepts input |
| 2 | Click Login | ✅ Success, redirects to /products |
| 3 | Verify navbar changes | Shows logged-in state |
| 4 | Check localStorage | JWT token is stored |

### 2.3 Login - Invalid Credentials
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Enter: client@test.com / wrongpassword | Form accepts input |
| 2 | Click Login | ❌ Error message displayed |
| 3 | Verify stays on page | No redirect, form still visible |

### 2.4 Register Form Display
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Go to /auth/register | Registration form displays |
| 2 | Verify form fields | Name, Email, Password, Role selector |
| 3 | Role options | CLIENT and SELLER options available |

### 2.5 Register - New User
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Fill: "New User", newuser123@test.com, password123, CLIENT | Form validates |
| 2 | Click Register | ✅ Success message |
| 3 | Verify redirect | Redirects to /auth/login |

### 2.6 Register - Duplicate Email (Bug Fix Verification)
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Fill: "Duplicate", client@test.com, password123, CLIENT | Form validates |
| 2 | Click Register | ❌ Error: "Email already registered" |
| 3 | Verify stays on page | No redirect |

---

## Test Category 3: Product Browsing

### 3.1 Product List Display
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Go to /products | Product grid loads |
| 2 | Verify product cards | Shows: Image, Name, Price, Add to Cart button |
| 3 | Verify pagination | Page controls visible (if > 12 products) |

### 3.2 Product Search
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Enter "Nokia" in search box | Filters as you type or on submit |
| 2 | Verify results | Only Nokia 3310 shows |
| 3 | Clear search | All products show again |

### 3.3 Product Filter by Category
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Select "Electronics" category | Filters to electronics only |
| 2 | Verify results | Shows Nokia 3310, Floppy disk |
| 3 | Select "Food" | Shows only Bazooka gum |

### 3.4 Product Filter by Price Range
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Set min price: $1, max: $10 | Filters products |
| 2 | Verify results | Shows Bazooka ($0.99) and Floppy ($2.59) |
| 3 | Clear filters | All products show |

### 3.5 Product Detail Page
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click on Nokia 3310 card | Navigates to /products/{id} |
| 2 | Verify detail page | Shows: Full image, Description, Price, Stock, Add to Cart |
| 3 | Verify stock display | Shows "In Stock: 10" or similar |

### 3.6 Add to Cart from Product Detail
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Login as client@test.com | Logged in |
| 2 | Go to Nokia 3310 detail page | Detail page loads |
| 3 | Select quantity: 2 | Quantity selector works |
| 4 | Click "Add to Cart" | ✅ Success toast/message |
| 5 | Verify cart badge | Cart icon shows updated count |

---

## Test Category 4: Shopping Cart UI

### 4.1 Cart Page Display
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Login as client@test.com | Logged in |
| 2 | Go to /cart | Cart page loads |
| 3 | Verify layout | Shows cart items, quantities, prices, subtotal |

### 4.2 Cart Item Display
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View cart with items | Each item shows: Image, Name, Price, Quantity, Subtotal |
| 2 | Verify remove button | "X" or "Remove" button visible for each item |

### 4.3 Update Cart Quantity
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Change Nokia quantity from 2 to 3 | Quantity updates |
| 2 | Verify subtotal updates | Line subtotal recalculates |
| 3 | Verify cart total updates | Cart total recalculates |

### 4.4 Cart Stock Validation (Bug Fix Verification)
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Try to set Floppy disk quantity to 10 (stock: 5) | Input changes |
| 2 | Verify validation | ❌ Error: "Only 5 available" or similar |
| 3 | Quantity stays at max | Cannot exceed stock |

### 4.5 Remove Cart Item
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click remove on one item | Item removed from cart |
| 2 | Verify cart updates | Total recalculates |
| 3 | Verify cart badge | Navbar badge updates |

### 4.6 Empty Cart Display
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Remove all items from cart | Cart becomes empty |
| 2 | Verify empty state | Shows "Your cart is empty" message |
| 3 | Verify CTA | Shows "Continue Shopping" link |

### 4.7 Cart Persistence
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Add items to cart | Items added |
| 2 | Refresh page (F5) | Cart still has items |
| 3 | Logout and login again | Cart items preserved |

---

## Test Category 5: Checkout Flow

### 5.1 Proceed to Checkout
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | With items in cart, click "Checkout" | Navigates to /checkout |
| 2 | Verify checkout page | Shows order summary, shipping form |

### 5.2 Checkout Form Validation
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Try to submit with empty fields | ❌ Validation errors shown |
| 2 | Verify required fields | Address, City, Postal Code required |

### 5.3 Complete Checkout (Pay on Delivery)
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Fill shipping address | Form validates |
| 2 | Verify payment method | "Pay on Delivery" selected (only option) |
| 3 | Click "Place Order" | ✅ Order created |
| 4 | Verify redirect | Redirects to order confirmation or /orders |
| 5 | Verify cart cleared | Cart is now empty |

### 5.4 Checkout with Empty Cart
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | With empty cart, go to /checkout | Should show error or redirect |
| 2 | Verify behavior | Cannot checkout with empty cart |

---

## Test Category 6: Order Management UI

### 6.1 Order List Display
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Go to /orders | Orders list page loads |
| 2 | Verify order cards | Shows: Order #, Date, Status, Total |
| 3 | Verify order sorting | Most recent first |

### 6.2 Order Detail Page
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click on an order | Navigates to /orders/{id} |
| 2 | Verify detail display | Shows: Items, Quantities, Prices, Shipping info |
| 3 | Verify status display | Status badge (CONFIRMED, CANCELLED, etc.) |

### 6.3 Cancel Order
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | On CONFIRMED order, click "Cancel" | Confirmation dialog appears |
| 2 | Confirm cancellation | ✅ Order status changes to CANCELLED |
| 3 | Verify button changes | Cancel button hidden/disabled |
| 4 | Verify "Redo" appears | Redo button becomes visible |

### 6.4 Redo Cancelled Order
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | On CANCELLED order, click "Redo" | Confirmation or immediate action |
| 2 | Verify new order created | ✅ New order with new order number |
| 3 | Verify redirect | Redirects to new order detail |
| 4 | Verify new status | New order is CONFIRMED |

### 6.5 Delete/Remove Order (if available)
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | On old order, click "Delete" | Confirmation dialog |
| 2 | Confirm delete | ✅ Order removed from list |

### 6.6 Order Filtering/Tabs
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "All Orders" tab | Shows all orders |
| 2 | Click "Confirmed" filter | Shows only CONFIRMED orders |
| 3 | Click "Cancelled" filter | Shows only CANCELLED orders |

---

## Test Category 7: Profile & Stats

### 7.1 Profile Page Display
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Login and go to /profile | Profile page loads |
| 2 | Verify user info | Shows: Name, Email, Role |
| 3 | Verify avatar | Default avatar or uploaded image |

### 7.2 Buyer Stats Display
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | As buyer, view profile | Stats section visible |
| 2 | Verify stats shown | Total Orders, Total Spent, etc. |
| 3 | Verify accuracy | Numbers match order history |

### 7.3 Seller Stats Display
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Login as seller@test.com | Logged in |
| 2 | Go to /profile | Profile page loads |
| 3 | Verify seller stats | Total Products, Total Sales, Revenue |

### 7.4 Edit Profile (if available)
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Edit Profile" | Edit form appears |
| 2 | Change name | Form accepts |
| 3 | Save changes | ✅ Profile updated |

---

## Test Category 8: Seller Dashboard

### 8.1 Dashboard Access
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Login as seller@test.com | Logged in |
| 2 | Navigate to /seller/dashboard | Dashboard loads |
| 3 | Verify content | Shows seller products, stats |

### 8.2 Product List (Seller's Products)
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View dashboard | Lists seller's products only |
| 2 | Verify product info | Shows: Name, Price, Stock, Actions |
| 3 | Verify edit/delete buttons | Edit and Delete buttons visible |

### 8.3 Create New Product
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Add Product" | Navigates to /seller/product-form |
| 2 | Fill product details | Name, Description, Price, Stock, Category |
| 3 | Upload image (if required) | Image upload works |
| 4 | Click "Save" | ✅ Product created |
| 5 | Verify in dashboard | New product appears in list |

### 8.4 Edit Product
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Edit" on a product | Navigates to /seller/product-form/{id} |
| 2 | Verify form pre-filled | Current values loaded |
| 3 | Change price | Form accepts |
| 4 | Save changes | ✅ Product updated |

### 8.5 Delete Product
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click "Delete" on a product | Confirmation dialog |
| 2 | Confirm deletion | ✅ Product removed |
| 3 | Verify removal | Product gone from list |

---

## Test Category 9: Responsive Design

### 9.1 Mobile View (< 768px)
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Resize browser to mobile width | Layout adjusts |
| 2 | Verify navbar | Hamburger menu appears |
| 3 | Verify product grid | Single column or 2-column |
| 4 | Verify cart page | Usable on mobile |

### 9.2 Tablet View (768px - 1024px)
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Resize to tablet width | Layout adjusts |
| 2 | Verify product grid | 2-3 columns |
| 3 | Verify checkout form | Readable and usable |

### 9.3 Desktop View (> 1024px)
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Full desktop width | Full layout visible |
| 2 | Verify product grid | 4+ columns |
| 3 | Verify all features | Everything accessible |

---

## Test Category 10: Error Handling

### 10.1 Network Error Handling
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Stop backend services | Services down |
| 2 | Try to load products | Error message displayed |
| 3 | Verify no crash | App doesn't crash |

### 10.2 404 Page / Invalid Routes
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Go to /nonexistent-page | Redirects to /products |
| 2 | Go to /orders/invalid-id | Error message or redirect |

### 10.3 Session Expiry
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Login and wait for token expiry | Token expires |
| 2 | Try to access /cart | Redirects to login |
| 3 | Verify message | "Session expired" or similar |

### 10.4 Form Validation Errors
| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Submit login with invalid email format | ❌ "Invalid email" error |
| 2 | Submit register with short password | ❌ Password requirements shown |
| 3 | Submit checkout without required fields | ❌ Field-specific errors |

---

# 📊 Test Execution Checklist

## Pre-Test Setup
```bash
# 1. Ensure all backend services are running
cd /Users/othmane.afilali/Desktop/buy-02
./start_all.sh

# 2. Wait for services (about 60-90 seconds)
# Check: http://localhost:8761 (Eureka dashboard)

# 3. Verify frontend is running
# Check: http://localhost:4200

# 4. Seed test data (if not already done)
node seed_mock_data.js
```

## Test Execution Order

| Order | Category | Depends On |
|-------|----------|------------|
| 1 | App Loading & Navigation | None |
| 2 | Authentication UI | #1 |
| 3 | Product Browsing | #1 |
| 4 | Shopping Cart UI | #2, #3 |
| 5 | Checkout Flow | #4 |
| 6 | Order Management UI | #5 |
| 7 | Profile & Stats | #2 |
| 8 | Seller Dashboard | #2 |
| 9 | Responsive Design | #1-8 |
| 10 | Error Handling | #1-8 |

---

# ✅ Results Template

Copy and fill during testing:

```
## Test Results - [DATE]

### Summary
| Category | Passed | Failed | Blocked |
|----------|--------|--------|---------|
| 1. App Loading | /5 | | |
| 2. Authentication | /6 | | |
| 3. Product Browsing | /6 | | |
| 4. Shopping Cart | /7 | | |
| 5. Checkout Flow | /4 | | |
| 6. Order Management | /6 | | |
| 7. Profile & Stats | /4 | | |
| 8. Seller Dashboard | /5 | | |
| 9. Responsive Design | /3 | | |
| 10. Error Handling | /4 | | |
| **TOTAL** | **/50** | | |

### Issues Found
1. [Issue description]
2. [Issue description]

### Notes
- [Any observations]
```

---

# 🔧 Bug Fix Verifications

These tests specifically verify the two fixes we applied:

## Fix #1: Duplicate Email Registration
- **Test 2.6**: Register with existing email
- **Expected**: Error message "Email already registered"
- **Status**: [ ] PASS / [ ] FAIL

## Fix #2: Cart Quantity > Stock
- **Test 4.4**: Cart Stock Validation  
- **Expected**: Cannot add more than available stock
- **Status**: [ ] PASS / [ ] FAIL

---

**Ready for review. Approve to begin testing.**
