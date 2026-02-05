# Server-Side Authorization Implementation

## Overview

This document describes the server-side security checks implemented for user capabilities, order ownership, and mutable status rules in the buy-02 e-commerce platform.

## Date Implemented

February 5, 2026

## Problem Statement

Before this implementation, the order-service had security vulnerabilities:

- **No ownership validation**: Users could access other users' orders by changing the userId in the URL
- **No role-based access control**: No distinction between SELLER and CLIENT capabilities
- **Client-side only validation**: Status rules were only enforced on the frontend

## What Was Achieved

### 1. JWT-Based Authentication

The order-service now extracts and validates user identity from JWT tokens:

- User ID
- Email
- Role (SELLER/CLIENT)

### 2. Three Security Checks Implemented

#### A. User vs Seller Capabilities

- **Clients (Buyers)** can:
  - View their own orders (`/my-orders`)
  - Cancel their orders (if status allows)
  - Redo cancelled orders
  - Remove orders from their list
- **Sellers** can:
  - View orders containing their products (`/seller-orders`)
  - Update order status (CONFIRMED, PROCESSING, SHIPPED)
  - Access orders where they have products

#### B. Order Ownership Validation

- Users can only access/modify orders they own
- Buyers must be the `buyerId` of the order
- Sellers must be in the `sellerIds` set of the order
- 403 Forbidden returned for unauthorized access attempts

#### C. Mutable Status Rules

Server enforces when orders can be modified:

| Status     | Can Cancel | Can Redo | Can Remove |
| ---------- | ---------- | -------- | ---------- |
| PENDING    | ✅         | ❌       | ❌         |
| CONFIRMED  | ✅         | ❌       | ❌         |
| PROCESSING | ✅         | ❌       | ❌         |
| SHIPPED    | ❌         | ❌       | ❌         |
| DELIVERED  | ❌         | ❌       | ✅         |
| CANCELLED  | ❌         | ✅       | ✅         |
| RETURNED   | ❌         | ❌       | ✅         |

## Files Changed

### Backend (order-service)

#### New Files Created:

1. **`security/JwtService.java`**
   - Extracts user information from JWT tokens
   - Methods: `extractUserId()`, `extractRole()`, `extractUsername()`, `isTokenValid()`
   - Uses the same secret key as user-service for consistent token validation

2. **`security/JwtAuthenticationFilter.java`**
   - Spring Security filter that intercepts requests
   - Extracts JWT from Authorization header
   - Sets up SecurityContext with authenticated user

3. **`security/AuthenticatedUser.java`**
   - POJO representing the authenticated user
   - Contains: userId, email, role
   - Helper methods: `isSeller()`, `isClient()`

4. **`security/AuthorizationService.java`**
   - Central authorization logic
   - Methods:
     - `requireAuthentication()` - Ensures user is logged in
     - `validateUserOwnership(userId)` - Validates user matches request
     - `validateOrderOwnership(order)` - Ensures user is the buyer
     - `validateSellerInOrder(order)` - Ensures seller has products in order
     - `validateOrderAccess(order)` - Allows buyer OR seller access
     - `validateCanCancel(order)` - Checks cancellation rules
     - `validateCanRedo(order)` - Checks redo rules
     - `validateCanRemove(order)` - Checks removal rules
     - `validateSellerOnlyTransition(status)` - Restricts seller-only statuses
     - `requireSellerRole()` / `requireClientRole()` - Role enforcement

#### Modified Files:

5. **`config/SecurityConfig.java`**
   - Added JWT filter to security chain
   - Added `@EnableMethodSecurity` for future @PreAuthorize support
   - Updated request matchers for proper endpoint paths

6. **`controller/OrderController.java`**
   - **REMOVED** userId from path parameters (now uses authenticated user)
   - All endpoints now call `authService` for validation
   - Added new endpoint: `PUT /{orderId}/status` for seller status updates
   - Audit trail now uses authenticated user info

7. **`controller/CartController.java`**
   - **REMOVED** userId from all path parameters
   - All cart operations now use authenticated user's ID
   - Simplified API: `/cart` instead of `/cart/{userId}`

### Frontend (buy-01-ui)

8. **`core/services/order.service.ts`**
   - Added `updateOrderStatus()` method for seller status updates
   - Added `getNextSellerStatus()` to determine next status in seller workflow
   - Added `canSellerUpdateStatus()` to check if seller can update order
   - Updated comments to clarify server-side authorization

9. **`features/orders/order-detail/order-detail.ts`**
   - Added `isSellerInOrder` computed signal
   - Added `isBuyer` computed signal
   - Added `nextSellerStatus` and `canSellerUpdate` computed signals
   - Added `updateStatus()` method for seller actions
   - Added `getSellerActionLabel()` and `getSellerActionIcon()` helpers

10. **`features/orders/order-detail/order-detail.html`**
    - Added conditional buyer/seller action sections
    - Added seller "Confirm", "Process", "Ship" buttons
    - Added seller badge indicator

11. **`features/orders/order-detail/order-detail.css`**
    - Added `.seller-badge` styling

12. **`features/orders/order-list/order-list.ts`**
    - Added `canSellerUpdate()`, `getNextSellerStatus()`, `getSellerActionLabel()`
    - Added `updateOrderStatus()` method for quick seller actions

13. **`features/orders/order-list/order-list.html`**
    - Split action buttons by viewMode (buyer vs seller)
    - Added seller status update buttons in seller view

## Frontend Features

### Buyer View

- View own orders only (`/my-orders`)
- Cancel orders (PENDING, CONFIRMED, PROCESSING)
- Redo cancelled orders
- Remove completed orders from list

### Seller View

- Toggle to "Sales" tab to see orders with their products
- Quick action buttons to update order status:
  - **Confirm** (PENDING → CONFIRMED)
  - **Process** (CONFIRMED → PROCESSING)
  - **Ship** (PROCESSING → SHIPPED)
- Seller badge indicator when viewing order details

### Role-Based UI

```typescript
// Computed signals automatically show/hide UI elements
readonly isSeller = computed(() => this.authService.isSeller());
readonly isSellerInOrder = computed(() => {
  const order = this.order();
  const userId = this.authService.currentUser()?.id;
  return this.authService.isSeller() && order.sellerIds?.includes(userId);
});
readonly canSellerUpdate = computed(() => {
  return this.isSellerInOrder() && this.orderService.canSellerUpdateStatus(order);
});
```

## API Changes

### Before (Insecure)

```
GET  /orders/my-orders/{userId}
GET  /orders/seller-orders/{sellerId}
GET  /cart/{userId}
POST /cart/{userId}/items
PUT  /cart/{userId}/items/{productId}?quantity=X
```

### After (Secure)

```
GET  /orders/my-orders           # Uses authenticated user
GET  /orders/seller-orders       # Uses authenticated seller
PUT  /orders/{orderId}/status    # Seller only, new endpoint
GET  /cart                       # Uses authenticated user
POST /cart/items                 # Uses authenticated user
PUT  /cart/items/{productId}     # Request body: { quantity }
```

## How It Works

### Authentication Flow

1. User logs in → receives JWT token
2. Frontend stores token and sends it with every request
3. `JwtAuthenticationFilter` intercepts request
4. Extracts user info from token
5. Creates `AuthenticatedUser` and sets in SecurityContext

### Authorization Flow

1. Controller receives request
2. Calls `authService.requireAuthentication()` or similar
3. `AuthorizationService` checks SecurityContext
4. Validates ownership/role/status rules
5. Returns 403 Forbidden if validation fails

## Error Responses

| HTTP Code        | Meaning                                      |
| ---------------- | -------------------------------------------- |
| 401 Unauthorized | No valid JWT token provided                  |
| 403 Forbidden    | User doesn't have permission for this action |
| 400 Bad Request  | Business rule violation (wrong status, etc.) |
| 404 Not Found    | Order/Cart not found                         |

## Testing

To test the implementation:

1. **Ownership Test**: Try accessing another user's order
   - Expected: 403 Forbidden

2. **Role Test**: As a CLIENT, try to update order status
   - Expected: 403 Forbidden

3. **Status Test**: Try to cancel a SHIPPED order
   - Expected: 400 Bad Request with clear error message

## Security Considerations

- JWT secret key must match between user-service and order-service
- Token expiration is validated
- All audit trails now use server-side user info (cannot be spoofed)
- No sensitive data exposed in error messages

## Frontend Authorization UI (Defense in Depth)

The frontend implements **show/hide button logic** and **friendly error messages** as a UX layer, while assuming attackers can still call the API directly (backend validates everything).

### Button Visibility Rules

| Button/Action        | Visibility Condition                                             | Location                 |
| -------------------- | ---------------------------------------------------------------- | ------------------------ |
| Cancel Order         | `@if (canCancel(order))` - only for PENDING/CONFIRMED/PROCESSING | order-list, order-detail |
| Redo Order           | `@if (canRedo(order))` - only for CANCELLED                      | order-list, order-detail |
| Remove Order         | `@if (viewMode() === 'buyer')` - buyers only                     | order-list               |
| Confirm/Process/Ship | `@if (canSellerUpdate(order))` - sellers only                    | order-list, order-detail |
| Seller View Tab      | `@if (isSeller())` - sellers see both tabs                       | order-list               |
| Seller Badge         | `@if (isSellerInOrder() && !isBuyer())`                          | order-detail             |

### Friendly Error Messages

All API errors display user-friendly messages via snackbar notifications:

```typescript
// Component-level error handling
error: (error) => {
  let errorMessage = "Failed to update order status";
  if (error.status === 403) {
    errorMessage = "You do not have permission to update this order";
  } else if (error.error?.message) {
    errorMessage = error.error.message;
  }
  this.snackBar.open(errorMessage, "Close", { duration: 4000 });
};
```

### Global Error Interceptor

The `error.interceptor.ts` handles common HTTP errors globally:

| HTTP Status | User Message                                                          |
| ----------- | --------------------------------------------------------------------- |
| 0 (Network) | "Cannot connect to server. Please check your internet connection."    |
| 400         | Extracted from backend or "Invalid request. Please check your input." |
| 401         | "Your session has expired. Please login again." + auto logout         |
| 403         | "You do not have permission to perform this action."                  |
| 404         | Extracted from backend or "The requested resource was not found."     |

### Defense in Depth Strategy

```
┌─────────────────────────────────────────────────────────────┐
│  Layer 1: UI (Frontend)                                     │
│  • Hide buttons users can't use (UX convenience)            │
│  • Show friendly error messages                             │
│  • Assume this layer CAN be bypassed                        │
├─────────────────────────────────────────────────────────────┤
│  Layer 2: API Gateway                                       │
│  • Route requests to services                               │
│  • Pass JWT token in Authorization header                   │
├─────────────────────────────────────────────────────────────┤
│  Layer 3: Backend (order-service)                           │
│  • Validate JWT token                                       │
│  • Check ownership (buyerId/sellerIds)                      │
│  • Enforce role-based permissions                           │
│  • Validate business rules (status transitions)             │
│  • Return 401/403/400 for violations                        │
│  • THIS IS THE REAL SECURITY LAYER                          │
└─────────────────────────────────────────────────────────────┘
```

## Future Improvements

- Add `@PreAuthorize` annotations for declarative security
- Implement rate limiting
- Add security audit logging
- Consider refresh token mechanism
