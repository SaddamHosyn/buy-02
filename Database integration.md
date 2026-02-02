# Buy-02 Database Integration Guide

> **Version:** 1.1.0  
> **Date:** February 1, 2026  
> **Author:** Database Owner  
> **Status:** Ready for Team Review

---

## Table of Contents

1. [Project Tasks Overview](#1-project-tasks-overview)
2. [Team Onboarding](#2-team-onboarding)
3. [Database Technology](#3-database-technology)
4. [Collections Summary](#4-collections-summary)
5. [Entity Definitions](#5-entity-definitions)
   - [Order Collection](#order-collection)
   - [Cart Collection](#cart-collection)
   - [Wishlist Collection](#wishlist-collection)
   - [Product Collection (Enhanced)](#product-collection-enhanced)
6. [Relationships](#6-relationships)
7. [Snapshotted Fields](#7-snapshotted-fields-at-checkout)
8. [Index Strategy](#8-index-strategy)
9. [Query Patterns](#9-query-patterns)
10. [Status Workflows](#10-status-workflows)
11. [API Contract for Backend Team](#11-api-contract-for-backend-team)
12. [Example Documents](#12-example-documents)
13. [Database Verification Guide](#13-database-verification-guide)
14. [Files Reference](#14-files-reference)

---

# 1. Project Tasks Overview

## Frontend Owner (Angular)

- Product search & filtering UI: keyword search + filter controls, results view, empty/loading/error states.
- Cart & checkout UI: cart page (add/remove/update quantities) and checkout flow that completes purchase using "pay on delivery."
- Orders UI: orders list/detail pages to follow order status; separate user vs seller views; orders search; actions (remove/cancel/redo) exposed in the UI based on allowed states.
- Profile UI: client profile (best products, most buying products, total spent) and seller profile (best-selling products, total gained).
- Validation + error handling + responsive design across all new screens and flows.

## Backend Owner (Microservices / Spring Boot)

- Orders Microservice: endpoints to track order status, list orders for users and sellers with search, and manage orders (remove/cancel/redo).
- Shopping cart APIs: server-side cart read/write (get cart, add item, update quantity, remove item, clear cart) and "checkout (pay on delivery) → create order."
- Product search/filter APIs: implement keyword search and filtering options required by buy-02 (extend Product service).
- Profile/stat APIs: compute and expose the data needed for user/seller profiles (spent/gained totals, best products).
- Security parity with buy-01: role-based rules and safe data exposure (don't leak sensitive fields).

## Database Owner (Schema + Integrity)

- Design and implement the new persistence needed for buy-02 ("add necessary tables and fields"), including cart and orders data models.
- Model orders to support: status tracking, user vs seller order queries (with search), and order management actions (cancel/redo/remove) with auditable fields (timestamps, status history if you choose).
- Add indexes/fields that make product search/filtering and order searching efficient.
- Deliver a written data contract to the team: entities, required fields, relationships, and which values are snapshotted at checkout (e.g., priceAtPurchase).

## Deployment Owner (CI/CD + Quality Gates)

- Jenkins CI/CD: auto-fetch on commit, build, test, and deploy the platform; ensure the pipeline fails when tests fail.
- Testing in pipeline: JUnit for backend + Jasmine/Karma for Angular frontend, wired into Jenkins stages.
- SonarQube integration: run analysis in CI and fail the pipeline if quality/security issues are detected (quality gate).
- Operational requirements: deployment strategy + rollback plan + build/deploy notifications (email/Slack).

## Team Handoffs (Who Needs What)

| From       | To       | Deliverable                                                     |
| ---------- | -------- | --------------------------------------------------------------- |
| Database   | Backend  | Final schema + example documents + index plan                   |
| Backend    | Frontend | Endpoint list + request/response DTOs + error codes             |
| Deployment | Everyone | "Definition of done" for PRs (tests green, quality gate passes) |

---

## 🔧 Backend Team Handoff: What Database Owner Delivered

### ✅ Database Owner (COMPLETE)

| Deliverable                    | Status | Details                                                          |
| ------------------------------ | ------ | ---------------------------------------------------------------- |
| **Order/Cart/Wishlist Schema** | ✅     | Entities with all required fields in `order-service/`            |
| **Product Enhancements**       | ✅     | Added `category`, `tags[]`, text indexes on `name`/`description` |
| **Indexes for Performance**    | ✅     | 14 indexes on Orders, 5 on Carts, 3 on Wishlists, 5 on Products  |
| **Repository Interfaces**      | ✅     | Query method signatures defined (Spring Data MongoDB)            |
| **Data Contract**              | ✅     | This document                                                    |

### 🔧 Backend Team (YOUR TASKS)

#### 1. Product Search & Filtering (Extend `product-service`)

**What's Ready:**

- Fields: `price`, `quantity`, `category`, `tags[]`
- Text index on `name`, `description`
- Repository methods defined in `ProductRepository.java`

**What You Need to Build:**

```java
// ProductController.java - Add filter endpoint
@GetMapping("/search")
public Page<Product> searchProducts(
    @RequestParam(required = false) String q,          // text search
    @RequestParam(required = false) String category,
    @RequestParam(required = false) Double minPrice,
    @RequestParam(required = false) Double maxPrice,
    @RequestParam(required = false) List<String> tags,
    @RequestParam(required = false) Boolean inStock,
    Pageable pageable
) {
    // Combine filters in ProductService
}

// ProductService.java - Implement filter logic
public Page<Product> searchWithFilters(...) {
    // Use repository methods or custom Query
}
```

**Available Repository Methods:**

```java
Page<Product> searchByText(String text, Pageable p);
Page<Product> findByCategory(String category, Pageable p);
Page<Product> findByPriceBetween(Double min, Double max, Pageable p);
Page<Product> findByTagsContaining(String tag, Pageable p);
Page<Product> findByQuantityGreaterThan(Integer qty, Pageable p);
```

#### 2. Order Service (New `order-service`)

**What's Ready:**

- All entities: `Order`, `Cart`, `Wishlist`
- Repositories with query methods
- Index configuration

**What You Need to Build:**

- `OrderController.java` - REST endpoints
- `OrderService.java` - Business logic (checkout, cancel, redo)
- `CartController.java` - Cart CRUD endpoints
- `CartService.java` - Cart operations
- Security config for JWT validation

#### 3. User Profile Stats (Extend `user-service` or create endpoint)

**Queries Available:**

```java
// For buyer stats
orderRepository.findByBuyerId(userId, pageable);      // their orders
orderRepository.sumTotalByBuyerId(userId);            // total spent

// For seller stats
orderRepository.findBySellerId(sellerId, pageable);   // orders with their products
// Aggregate query for total gained, best products
```

---

# 2. Team Onboarding

## Quick Start for Team Members

### 1. Clone the Repository

```bash
git clone <repository-url>
cd buy-02
```

### 2. Get Database Credentials

Contact the **Database Owner** to get:

- Your personal MongoDB Atlas username/password
- Or use the shared development credentials

### 3. Setup Environment

```bash
# Copy the environment template
cp .env.example .env

# Edit .env with your credentials
# Replace YOUR_USERNAME and YOUR_PASSWORD
```

---

## Team Roles & Access

### 🗄️ Database Owner (Schema + Integrity)

**Responsibilities:**

- Designed and maintains database schema
- Reviews schema changes
- Manages MongoDB Atlas users

**Key Files:**

- This document (`Database integration.md`)
- `order-service/` - Order, Cart, Wishlist entities

---

### ⚙️ Backend Team

**Your databases:**
| Service | Database | Port | Collections |
|---------|----------|------|-------------|
| order-service | `orderdb` | 8084 | orders, carts, wishlists |
| user-service | `userdb` | 8081 | users |
| product-service | `productdb` | 8082 | products |
| media-service | `mediadb` | 8083 | media |

**Connection String Format:**

```
mongodb+srv://<username>:<password>@buy02-cluster.s0i27el.mongodb.net/<database>?retryWrites=true&w=majority
```

**Running Services Locally:**

```bash
# Start all services (requires Eureka first)
mvn -pl service-registry spring-boot:run

# Then start other services
mvn -pl user-service spring-boot:run
mvn -pl product-service spring-boot:run
mvn -pl order-service spring-boot:run -DskipTests
mvn -pl api-gateway spring-boot:run
```

---

### 🎨 Frontend Team

**API Gateway URL:** `http://localhost:8080/api`

**Key Endpoints (via API Gateway):**
| Feature | Endpoint Pattern |
|---------|------------------|
| Products | `/api/products/*` |
| Users | `/api/users/*` |
| Orders | `/api/orders/*` |
| Cart | `/api/cart/*` |
| Media | `/api/media/*` |

**Frontend Location:** `buy-01-ui/`

---

### 🚀 DevOps / Deployment Team

**Environment Variables Required:**

```bash
# MongoDB (per service)
SPRING_DATA_MONGODB_URI

# Eureka
EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE

# JWT (shared across services)
JWT_SECRET_KEY

# Kafka (optional)
SPRING_KAFKA_BOOTSTRAP_SERVERS
```

**Docker Files:**

- Each service has a `Dockerfile`
- See `start_docker.sh` for Docker Compose setup

**MongoDB Atlas Cluster:**

- Cluster: `buy02-cluster`
- Region: AWS eu-north-1 (Stockholm)
- Tier: M0 (Free)

---

## MongoDB Atlas Access

### Viewing Data

1. Go to https://cloud.mongodb.com/
2. Login with credentials from Database Owner
3. Click cluster → "Browse Collections"

### Connection via MongoDB Compass

```
mongodb+srv://<username>:<password>@buy02-cluster.s0i27el.mongodb.net/
```

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT (Angular)                        │
│                        buy-01-ui (port 4200)                    │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                      API GATEWAY (port 8080)                    │
│                    Routes requests to services                   │
└─────────────────────────────────────────────────────────────────┘
                                  │
        ┌─────────────────────────┼─────────────────────────┐
        ▼                         ▼                         ▼
┌───────────────┐       ┌───────────────┐       ┌───────────────┐
│ user-service  │       │product-service│       │ order-service │
│   (8081)      │       │    (8082)     │       │    (8084)     │
│   userdb      │       │   productdb   │       │   orderdb     │
└───────────────┘       └───────────────┘       └───────────────┘
                                │
                        ┌───────────────┐
                        │ media-service │
                        │    (8083)     │
                        │    mediadb    │
                        └───────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                   SERVICE REGISTRY (Eureka)                     │
│                         (port 8761)                             │
└─────────────────────────────────────────────────────────────────┘
```

---

# 3. Database Technology

| Aspect       | Choice                          |
| ------------ | ------------------------------- |
| **Database** | MongoDB                         |
| **ODM**      | Spring Data MongoDB             |
| **Version**  | MongoDB 6.x+ recommended        |
| **Indexes**  | Programmatic + Annotation-based |

### Connection Details

```properties
# Order Service Database
spring.data.mongodb.uri=mongodb://localhost:27017/orderdb
spring.data.mongodb.database=orderdb

# Product Service Database (existing)
spring.data.mongodb.uri=mongodb://localhost:27017/productdb
```

### Design Principles

1. **MongoDB Document Model** - Embedded documents for related data (items, history)
2. **Denormalization** - `sellerIds` in Order for efficient seller queries
3. **Snapshotting** - Product details frozen at checkout for historical accuracy
4. **Soft Deletes** - Orders are hidden, not deleted (audit compliance)
5. **Audit Trail** - Full status history with timestamps and actors

---

# 4. Collections Summary

| Collection  | Service         | Description                                    |
| ----------- | --------------- | ---------------------------------------------- |
| `orders`    | order-service   | Completed purchase records                     |
| `carts`     | order-service   | User shopping carts with status                |
| `wishlists` | order-service   | User wishlists (bonus feature)                 |
| `products`  | product-service | Product catalog (enhanced with search indexes) |
| `users`     | user-service    | User accounts (existing)                       |
| `media`     | media-service   | Product images (existing)                      |

---

# 5. Entity Definitions

## Order Collection

**Collection Name:** `orders`  
**Service:** order-service  
**Purpose:** Store completed purchases with full audit trail

### Schema

```javascript
{
  _id: ObjectId,                    // MongoDB auto-generated ID
  orderNumber: String,              // UNIQUE, format: "ORD-YYYYMMDD-XXXXX"

  // === Buyer Information ===
  buyerId: String,                  // REQUIRED, INDEXED - Reference to users collection
  buyerName: String,                // SNAPSHOTTED at checkout
  buyerEmail: String,               // SNAPSHOTTED, INDEXED for search

  // === Order Items (Embedded Array) ===
  items: [
    {
      productId: String,            // Reference to products collection
      productName: String,          // SNAPSHOTTED
      productDescription: String,   // SNAPSHOTTED
      priceAtPurchase: Double,      // SNAPSHOTTED - Critical for accurate totals
      quantity: Integer,            // >= 1
      subtotal: Double,             // Calculated: priceAtPurchase * quantity
      sellerId: String,             // Reference to users collection (seller)
      sellerName: String,           // SNAPSHOTTED
      thumbnailMediaId: String      // SNAPSHOTTED - For order history display
    }
  ],

  // === Denormalized Seller IDs ===
  sellerIds: [String],              // INDEXED - All seller IDs in this order

  // === Pricing (All Snapshotted) ===
  subtotal: Double,                 // Sum of item subtotals
  shippingCost: Double,             // Default: 0.0
  taxAmount: Double,                // Default: 0.0
  discountAmount: Double,           // Default: 0.0
  totalAmount: Double,              // REQUIRED - Final amount

  // === Payment ===
  paymentMethod: String,            // "PAY_ON_DELIVERY" for buy-02
  paymentStatus: String,            // "PENDING" | "COMPLETED" | "REFUNDED"

  // === Shipping ===
  shippingAddress: {
    fullName: String,               // REQUIRED
    addressLine1: String,           // REQUIRED
    addressLine2: String,           // Optional
    city: String,                   // REQUIRED
    postalCode: String,             // REQUIRED
    country: String,                // REQUIRED
    phoneNumber: String             // Optional
  },
  deliveryNotes: String,            // Optional buyer notes

  // === Status & Tracking ===
  status: String,                   // INDEXED - See OrderStatus enum
  statusHistory: [                  // Full audit trail
    {
      previousStatus: String,       // null for initial
      newStatus: String,
      changedAt: DateTime,
      changedBy: String,            // User ID who made change
      changedByRole: String,        // "CLIENT" | "SELLER" | "SYSTEM"
      reason: String                // Optional explanation
    }
  ],
  estimatedDeliveryDate: DateTime,
  actualDeliveryDate: DateTime,

  // === Audit Fields ===
  createdAt: DateTime,              // INDEXED
  updatedAt: DateTime,
  originalOrderId: String,          // If created via "redo" from cancelled order

  // === Soft Delete ===
  isRemoved: Boolean,               // Default: false
  removedAt: DateTime,
  removedBy: String
}
```

### OrderStatus Enum Values

| Status       | Description                          | Can Cancel? | Next States              |
| ------------ | ------------------------------------ | ----------- | ------------------------ |
| `PENDING`    | Order created, awaiting confirmation | ✅ Yes      | CONFIRMED, CANCELLED     |
| `CONFIRMED`  | Seller confirmed the order           | ✅ Yes      | PROCESSING, CANCELLED    |
| `PROCESSING` | Being prepared for shipment          | ✅ Yes      | SHIPPED, CANCELLED       |
| `SHIPPED`    | In transit to customer               | ❌ No       | DELIVERED                |
| `DELIVERED`  | Successfully delivered               | ❌ No       | RETURNED                 |
| `CANCELLED`  | Order was cancelled                  | N/A         | (redo creates new order) |
| `RETURNED`   | Customer returned items              | N/A         | -                        |

---

## Cart Collection

**Collection Name:** `carts`  
**Service:** order-service  
**Purpose:** Store user shopping carts (one per user) with status tracking

### Schema

```javascript
{
  _id: ObjectId,
  userId: String,                   // UNIQUE INDEX - One cart per user
  status: String,                   // INDEXED - "ACTIVE" | "PURCHASED" | "ABANDONED" | "MERGED"

  items: [
    {
      productId: String,            // Reference to products
      quantity: Integer,            // >= 1
      sellerId: String,             // For grouping/filtering
      addedAt: DateTime,
      updatedAt: DateTime,
      // Cached fields (refreshed when cart is viewed)
      cachedProductName: String,
      cachedPrice: Double
    }
  ],

  // Denormalized totals for quick display
  totalItems: Integer,              // Sum of all quantities
  cachedSubtotal: Double,           // Calculated from cached prices

  // Timestamps
  createdAt: DateTime,
  updatedAt: DateTime,              // INDEXED - For abandoned cart cleanup
  lastAccessedAt: DateTime
}
```

### CartStatus Enum Values

| Status      | Description                                      |
| ----------- | ------------------------------------------------ |
| `ACTIVE`    | User is still shopping                           |
| `PURCHASED` | Cart has been checked out and converted to order |
| `ABANDONED` | No activity for extended period                  |
| `MERGED`    | Cart was merged (e.g., guest to logged-in user)  |

### Cart Lifecycle

1. **Created** when user adds first item (status: ACTIVE)
2. **Updated** as user modifies items
3. **PURCHASED** after successful checkout (cart cleared, status updated)
4. **ABANDONED** - Carts not updated in 30 days may be marked/purged

---

## Wishlist Collection

**Collection Name:** `wishlists`  
**Service:** order-service  
**Purpose:** Store saved products for future purchase (Bonus feature)

### Schema

```javascript
{
  _id: ObjectId,
  userId: String,                   // UNIQUE INDEX - One wishlist per user

  items: [
    {
      productId: String,            // Reference to products
      sellerId: String,             // For seller stats
      addedAt: DateTime,
      // Cached fields (refreshed when wishlist is viewed)
      cachedProductName: String,
      cachedPrice: Double,
      cachedInStock: Boolean,
      note: String,                 // Optional user note
      priority: Integer             // 1=High, 2=Medium, 3=Low
    }
  ],

  totalItems: Integer,              // Count of items

  // Timestamps
  createdAt: DateTime,
  updatedAt: DateTime
}
```

### Wishlist Features

- **One per user** - Persists across sessions
- **Move to Cart** - Items can be transferred to cart
- **Priority sorting** - Users can prioritize items
- **Seller stats** - Count how many users wishlisted a product

---

## Product Collection (Enhanced)

**Collection Name:** `products`  
**Service:** product-service  
**Purpose:** Product catalog with search, category, and tag filtering

### Enhanced Schema

```javascript
{
  _id: ObjectId,
  name: String,                     // TEXT INDEXED (weight: 3)
  description: String,              // TEXT INDEXED (weight: 1)
  price: Double,                    // INDEXED
  quantity: Integer,                // INDEXED
  userId: String,                   // INDEXED - Seller ID
  category: String,                 // INDEXED - Category for filtering
  tags: [String],                   // INDEXED - Tags for flexible filtering
  mediaIds: [String],
  createdAt: DateTime,              // INDEXED
  updatedAt: DateTime,
  score: Float                      // Text search score (populated by MongoDB)
}
```

### New Fields

| Field      | Type     | Required | Description                                                   |
| ---------- | -------- | -------- | ------------------------------------------------------------- |
| `category` | String   | No       | Product category for filtering                                |
| `tags`     | [String] | No       | Flexible tags for filtering (e.g., "sale", "new", "featured") |

**Suggested Categories:**

- Electronics
- Clothing
- Home & Garden
- Sports
- Books
- Other

**Example Tags:**

- `new` - Recently added products
- `sale` - Products on sale
- `featured` - Featured/promoted products
- `eco-friendly` - Environmentally friendly products
- `bestseller` - Top selling products

---

# 6. Relationships

```
┌─────────────────────────────────────────────────────────────────────┐
│                         RELATIONSHIP DIAGRAM                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   ┌──────────┐         1:1         ┌──────────┐                    │
│   │   User   │◄───────────────────►│   Cart   │                    │
│   │ (CLIENT) │                      └──────────┘                    │
│   └────┬─────┘                           │                          │
│        │                                 │ contains                 │
│        │ 1:N                             ▼                          │
│        │                          ┌──────────────┐                  │
│        │                          │  CartItem    │                  │
│        │                          │ (references) │                  │
│        │                          └──────┬───────┘                  │
│        │                                 │                          │
│        │                                 │ references               │
│        │                                 ▼                          │
│        │                          ┌──────────────┐                  │
│   ┌────┴─────┐      1:N           │   Product    │     1:N         │
│   │  Order   │◄──────────────────►│  (SELLER)   │◄────────────┐    │
│   └────┬─────┘     (items ref)    └──────────────┘             │    │
│        │                                                        │    │
│        │ contains                                               │    │
│        ▼                                                        │    │
│   ┌──────────────┐                                              │    │
│   │  OrderItem   │──────────────────────────────────────────────┘    │
│   │ (SNAPSHOTTED)│  copies product data at checkout                  │
│   └──────────────┘                                                   │
│                                                                      │
│   ┌──────────┐         N:M (via sellerIds)      ┌──────────┐        │
│   │   User   │◄────────────────────────────────►│  Order   │        │
│   │ (SELLER) │                                   └──────────┘        │
│   └──────────┘                                                       │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

### Reference Types

| From                | To           | Type         | Notes                             |
| ------------------- | ------------ | ------------ | --------------------------------- |
| Cart.userId         | User.\_id    | Reference    | One cart per user                 |
| CartItem.productId  | Product.\_id | Reference    | Live product data                 |
| CartItem.sellerId   | User.\_id    | Reference    | Product owner                     |
| Order.buyerId       | User.\_id    | Reference    | Order creator                     |
| Order.sellerIds[]   | User.\_id    | Denormalized | All sellers in order              |
| OrderItem.productId | Product.\_id | Reference    | Original product (may be deleted) |
| OrderItem.sellerId  | User.\_id    | Reference    | Product owner                     |
| OrderItem.\*        | Product.\*   | **SNAPSHOT** | Copied at checkout                |

---

# 7. Snapshotted Fields at Checkout

**CRITICAL**: These fields are **copied** from live data at checkout time and **never updated**, ensuring historical accuracy.

### OrderItem Snapshots (from Product)

| OrderItem Field      | Source              | Why Snapshot?                   |
| -------------------- | ------------------- | ------------------------------- |
| `productName`        | Product.name        | Product may be renamed          |
| `productDescription` | Product.description | May change                      |
| `priceAtPurchase`    | Product.price       | **CRITICAL** - Price may change |
| `sellerName`         | User.name (seller)  | Seller may rename account       |
| `thumbnailMediaId`   | Product.mediaIds[0] | Image may change                |

### Order Snapshots (from User)

| Order Field       | Source     | Why Snapshot?            |
| ----------------- | ---------- | ------------------------ |
| `buyerName`       | User.name  | User may change name     |
| `buyerEmail`      | User.email | Email may change         |
| `shippingAddress` | User input | Address at time of order |

### Calculation at Checkout

```
subtotal = Σ (item.priceAtPurchase × item.quantity)
totalAmount = subtotal + shippingCost + taxAmount - discountAmount
```

---

# 8. Index Strategy

### Order Collection Indexes

| Index Name              | Fields                             | Type     | Purpose                |
| ----------------------- | ---------------------------------- | -------- | ---------------------- |
| `orderNumber_1`         | orderNumber                        | Unique   | Order lookup           |
| `buyer_orders_idx`      | buyerId, createdAt DESC            | Compound | User order history     |
| `seller_orders_idx`     | sellerIds, status, createdAt DESC  | Compound | Seller order queries   |
| `status_orders_idx`     | status, createdAt DESC             | Compound | Status-based filtering |
| `order_text_search_idx` | orderNumber, buyerEmail, buyerName | Text     | Order search           |
| `removed_orders_idx`    | isRemoved, removedAt               | Compound | Cleanup jobs           |
| `original_order_idx`    | originalOrderId                    | Sparse   | Redo tracking          |

### Cart Collection Indexes

| Index Name          | Fields          | Type   | Purpose                   |
| ------------------- | --------------- | ------ | ------------------------- |
| `cart_user_idx`     | userId          | Unique | One cart per user         |
| `cart_status_idx`   | status          | Single | Status filtering          |
| `cart_updated_idx`  | updatedAt       | Single | Abandoned cart cleanup    |
| `cart_products_idx` | items.productId | Single | Product deletion handling |
| `cart_sellers_idx`  | items.sellerId  | Single | Seller deletion handling  |

### Wishlist Collection Indexes

| Index Name              | Fields          | Type   | Purpose                  |
| ----------------------- | --------------- | ------ | ------------------------ |
| `wishlist_user_idx`     | userId          | Unique | One wishlist per user    |
| `wishlist_products_idx` | items.productId | Single | Product wishlisted count |
| `wishlist_sellers_idx`  | items.sellerId  | Single | Seller stats             |

### Product Collection Indexes (Enhanced)

| Index Name              | Fields                   | Type     | Purpose             |
| ----------------------- | ------------------------ | -------- | ------------------- |
| `name_description_text` | name, description        | Text     | Keyword search      |
| `seller_date_idx`       | userId, createdAt DESC   | Compound | Seller products     |
| `price_date_idx`        | price, createdAt DESC    | Compound | Price filtering     |
| `stock_date_idx`        | quantity, createdAt DESC | Compound | In-stock products   |
| `price_1`               | price                    | Single   | Price range queries |
| `quantity_1`            | quantity                 | Single   | Stock filtering     |
| `category_1`            | category                 | Single   | Category filtering  |
| `tags_1`                | tags                     | Single   | Tag filtering       |
| `createdAt_1`           | createdAt                | Single   | Date sorting        |

---

# 9. Query Patterns

### For Backend Team - Common Query Examples

#### 1. User's Order History

```javascript
// MongoDB Query
db.orders.find({
  buyerId: "user123",
  isRemoved: { $ne: true }
}).sort({ createdAt: -1 })

// Spring Data Method
Page<Order> findByBuyerIdAndNotRemoved(String buyerId, Pageable pageable);
```

#### 2. Seller's Orders (containing their products)

```javascript
// MongoDB Query
db.orders.find({
  sellerIds: "seller456",
  status: "PENDING",
  isRemoved: { $ne: true }
}).sort({ createdAt: -1 })

// Spring Data Method
Page<Order> findBySellerIdAndStatus(String sellerId, OrderStatus status, Pageable pageable);
```

#### 3. Order Search (by order number or buyer email)

```javascript
// MongoDB Query
db.orders.find({
  sellerIds: "seller456",
  $or: [
    { orderNumber: { $regex: "ORD-2026", $options: "i" } },
    { buyerEmail: { $regex: "john", $options: "i" } }
  ]
})

// Spring Data Method
Page<Order> searchBySellerIdAndKeyword(String sellerId, String keyword, Pageable pageable);
```

#### 4. Product Text Search

```javascript
// MongoDB Query
db.products.find({
  $text: { $search: "wireless headphones" },
  quantity: { $gt: 0 }
}).sort({ score: { $meta: "textScore" } })

// Spring Data Method
Page<Product> searchByText(String searchText, Pageable pageable);
```

#### 5. Product Filtering

```javascript
// MongoDB Query
db.products.find({
  category: "Electronics",
  price: { $gte: 50, $lte: 200 },
  quantity: { $gt: 0 }
}).sort({ price: 1 })

// Spring Data Method
Page<Product> findWithFilters(String category, Double minPrice, Double maxPrice, Pageable pageable);
```

#### 6. Tag-based Filtering

```javascript
// MongoDB Query
db.products.find({
  tags: { $in: ["sale", "featured"] }
})

// Spring Data Method
Page<Product> findByTagsIn(List<String> tags, Pageable pageable);
```

---

# 10. Status Workflows

### Order Status State Machine

```
                    ┌─────────────────────────────────────────┐
                    │                                         │
                    ▼                                         │
┌─────────┐    ┌───────────┐    ┌────────────┐    ┌─────────┐│
│ PENDING │───►│ CONFIRMED │───►│ PROCESSING │───►│ SHIPPED ││
└────┬────┘    └─────┬─────┘    └──────┬─────┘    └────┬────┘│
     │               │                 │               │      │
     │               │                 │               │      │
     │    ┌──────────┴─────────────────┘               │      │
     │    │                                            │      │
     │    │ (cancel)                                   │      │
     │    ▼                                            ▼      │
     │ ┌───────────┐                             ┌───────────┐│
     └►│ CANCELLED │                             │ DELIVERED ││
       └─────┬─────┘                             └─────┬─────┘│
             │                                         │      │
             │ (redo - creates new order)              │      │
             └─────────────────────────────────────────┼──────┘
                                                       │
                                                       ▼
                                                 ┌───────────┐
                                                 │ RETURNED  │
                                                 └───────────┘
```

### Status Change Rules

| Current Status | Allowed Transitions        | Who Can Trigger             |
| -------------- | -------------------------- | --------------------------- |
| PENDING        | CONFIRMED, CANCELLED       | Seller, Buyer (cancel only) |
| CONFIRMED      | PROCESSING, CANCELLED      | Seller, Buyer (cancel only) |
| PROCESSING     | SHIPPED, CANCELLED         | Seller                      |
| SHIPPED        | DELIVERED                  | Seller, System              |
| DELIVERED      | RETURNED                   | Buyer (within window)       |
| CANCELLED      | _(redo creates new order)_ | Buyer                       |

---

# 11. API Contract for Backend Team

### Order Service Endpoints (Suggested)

| Method | Endpoint                  | Description                    |
| ------ | ------------------------- | ------------------------------ |
| POST   | `/api/orders/checkout`    | Create order from cart         |
| GET    | `/api/orders`             | List user's orders (paginated) |
| GET    | `/api/orders/{id}`        | Get order details              |
| GET    | `/api/orders/seller`      | List seller's orders           |
| GET    | `/api/orders/search?q=`   | Search orders                  |
| PATCH  | `/api/orders/{id}/cancel` | Cancel order                   |
| POST   | `/api/orders/{id}/redo`   | Recreate cancelled order       |
| DELETE | `/api/orders/{id}`        | Soft-delete (remove) order     |

### Cart Service Endpoints (Suggested)

| Method | Endpoint                      | Description          |
| ------ | ----------------------------- | -------------------- |
| GET    | `/api/cart`                   | Get user's cart      |
| POST   | `/api/cart/items`             | Add item to cart     |
| PATCH  | `/api/cart/items/{productId}` | Update item quantity |
| DELETE | `/api/cart/items/{productId}` | Remove item          |
| DELETE | `/api/cart`                   | Clear cart           |

### Wishlist Service Endpoints (Suggested)

| Method | Endpoint                                       | Description          |
| ------ | ---------------------------------------------- | -------------------- |
| GET    | `/api/wishlist`                                | Get user's wishlist  |
| POST   | `/api/wishlist/items`                          | Add item to wishlist |
| DELETE | `/api/wishlist/items/{productId}`              | Remove item          |
| POST   | `/api/wishlist/items/{productId}/move-to-cart` | Move to cart         |

### DTOs for Backend Team

#### CheckoutRequest

```java
public record CheckoutRequest(
    ShippingAddress shippingAddress,
    String deliveryNotes,
    String paymentMethod  // "PAY_ON_DELIVERY"
) {}
```

#### OrderResponse

```java
public record OrderResponse(
    String id,
    String orderNumber,
    String buyerName,
    List<OrderItemResponse> items,
    Double totalAmount,
    OrderStatus status,
    LocalDateTime createdAt,
    ShippingAddress shippingAddress
) {}
```

#### CartResponse

```java
public record CartResponse(
    String id,
    List<CartItemResponse> items,
    Integer totalItems,
    Double subtotal
) {}
```

---

# 12. Example Documents

### Order Document Example

```json
{
  "_id": "ord_507f1f77bcf86cd799439011",
  "orderNumber": "ORD-20260201-A3F5K",
  "buyerId": "usr_507f1f77bcf86cd799439012",
  "buyerName": "John Doe",
  "buyerEmail": "john.doe@example.com",
  "items": [
    {
      "productId": "prod_507f1f77bcf86cd799439013",
      "productName": "Wireless Headphones",
      "productDescription": "Bluetooth 5.0 headphones with noise cancellation",
      "priceAtPurchase": 79.99,
      "quantity": 2,
      "subtotal": 159.98,
      "sellerId": "usr_507f1f77bcf86cd799439014",
      "sellerName": "TechStore",
      "thumbnailMediaId": "media_507f1f77bcf86cd799439015"
    }
  ],
  "sellerIds": ["usr_507f1f77bcf86cd799439014"],
  "subtotal": 159.98,
  "shippingCost": 5.99,
  "taxAmount": 0.0,
  "discountAmount": 0.0,
  "totalAmount": 165.97,
  "paymentMethod": "PAY_ON_DELIVERY",
  "paymentStatus": "PENDING",
  "shippingAddress": {
    "fullName": "John Doe",
    "addressLine1": "123 Main Street",
    "addressLine2": "Apt 4B",
    "city": "New York",
    "postalCode": "10001",
    "country": "USA",
    "phoneNumber": "+1-555-123-4567"
  },
  "deliveryNotes": "Please leave at door",
  "status": "PENDING",
  "statusHistory": [
    {
      "previousStatus": null,
      "newStatus": "PENDING",
      "changedAt": "2026-02-01T10:30:00Z",
      "changedBy": "usr_507f1f77bcf86cd799439012",
      "changedByRole": "CLIENT",
      "reason": "Order created"
    }
  ],
  "estimatedDeliveryDate": "2026-02-08T18:00:00Z",
  "createdAt": "2026-02-01T10:30:00Z",
  "updatedAt": "2026-02-01T10:30:00Z",
  "isRemoved": false
}
```

### Cart Document Example

```json
{
  "_id": "cart_507f1f77bcf86cd799439019",
  "userId": "usr_507f1f77bcf86cd799439012",
  "status": "ACTIVE",
  "items": [
    {
      "productId": "prod_507f1f77bcf86cd799439013",
      "quantity": 2,
      "sellerId": "usr_507f1f77bcf86cd799439014",
      "addedAt": "2026-02-01T09:00:00Z",
      "updatedAt": "2026-02-01T09:30:00Z",
      "cachedProductName": "Wireless Headphones",
      "cachedPrice": 79.99
    }
  ],
  "totalItems": 2,
  "cachedSubtotal": 159.98,
  "createdAt": "2026-02-01T09:00:00Z",
  "updatedAt": "2026-02-01T09:30:00Z"
}
```

### Wishlist Document Example

```json
{
  "_id": "wish_507f1f77bcf86cd799439020",
  "userId": "usr_507f1f77bcf86cd799439012",
  "items": [
    {
      "productId": "prod_507f1f77bcf86cd799439021",
      "sellerId": "usr_507f1f77bcf86cd799439014",
      "addedAt": "2026-02-01T08:00:00Z",
      "cachedProductName": "Smart Watch",
      "cachedPrice": 299.99,
      "cachedInStock": true,
      "note": "Wait for sale",
      "priority": 1
    }
  ],
  "totalItems": 1,
  "createdAt": "2026-02-01T08:00:00Z",
  "updatedAt": "2026-02-01T08:00:00Z"
}
```

---

# 13. Database Verification Guide

## Option 1: Run Automated Tests (Recommended)

### Prerequisites

- Docker Desktop running (for Testcontainers)

### Run the tests

```bash
cd D:\Projects\buy-02
mvn test -pl order-service -Dtest=DatabaseVerificationTest
```

### Expected output

```
✅ Order created successfully!
✅ Buyer orders query works!
✅ Seller orders query works!
✅ Cart created successfully!
✅ Cart lookup by user works!

🎉 ALL DATABASE VERIFICATION TESTS PASSED!
```

---

## Option 2: Manual Testing via REST API

### Step 1: Start the Order Service

```bash
cd D:\Projects\buy-02
mvn spring-boot:run -pl order-service -DskipTests
```

Wait for: `Started OrderServiceApplication in X seconds`

### Step 2: Test the Endpoints

#### Health Check

```bash
curl http://localhost:8084/api/orders/health
```

**Expected response:**

```json
{
  "service": "order-service",
  "status": "UP",
  "database": "orderdb",
  "mongoStatus": "CONNECTED",
  "collections": ["orders", "carts", "wishlists"],
  "orderCount": 0,
  "cartCount": 0
}
```

#### Create Test Data

```bash
curl -X POST http://localhost:8084/api/orders/verify/create-test-data
```

#### Run Query Tests

```bash
curl http://localhost:8084/api/orders/verify/test-queries
```

---

## Option 3: MongoDB Compass / Atlas

### Using MongoDB Compass (GUI)

1. Download and install [MongoDB Compass](https://www.mongodb.com/products/compass)
2. Connect to: `mongodb+srv://<username>:<password>@buy02-cluster.s0i27el.mongodb.net/`
3. Look for database: `orderdb`
4. Check collections: `orders`, `carts`, `wishlists`

### Using MongoDB Atlas (Web)

1. Go to https://cloud.mongodb.com/
2. Click your cluster → "Browse Collections"
3. Select `orderdb` database

---

## What to Verify

### ✅ Indexes Created

**Orders Collection:** `orderNumber_1`, `buyer_orders_idx`, `seller_orders_idx`, `status_orders_idx`

**Carts Collection:** `cart_user_idx`, `cart_status_idx`, `cart_updated_idx`

**Wishlists Collection:** `wishlist_user_idx`, `wishlist_products_idx`

### ✅ Data Structure

- Order contains embedded `items[]` with snapshotted product data
- Order contains `statusHistory[]` for audit trail
- Cart has `status` field (ACTIVE, PURCHASED, etc.)

---

# 14. Files Reference

### New Files (order-service)

| File Path                                                            | Description             |
| -------------------------------------------------------------------- | ----------------------- |
| `order-service/pom.xml`                                              | Maven configuration     |
| `order-service/Dockerfile`                                           | Container build         |
| `order-service/src/main/resources/application.properties`            | Service configuration   |
| `order-service/src/main/java/.../model/Order.java`                   | Order entity            |
| `order-service/src/main/java/.../model/OrderItem.java`               | Order item embedded doc |
| `order-service/src/main/java/.../model/OrderStatus.java`             | Status enum             |
| `order-service/src/main/java/.../model/OrderStatusHistory.java`      | Audit history           |
| `order-service/src/main/java/.../model/Cart.java`                    | Cart entity             |
| `order-service/src/main/java/.../model/CartItem.java`                | Cart item embedded doc  |
| `order-service/src/main/java/.../model/CartStatus.java`              | Cart status enum        |
| `order-service/src/main/java/.../model/Wishlist.java`                | Wishlist entity         |
| `order-service/src/main/java/.../model/WishlistItem.java`            | Wishlist item           |
| `order-service/src/main/java/.../model/ShippingAddress.java`         | Address embedded doc    |
| `order-service/src/main/java/.../repository/OrderRepository.java`    | Order data access       |
| `order-service/src/main/java/.../repository/CartRepository.java`     | Cart data access        |
| `order-service/src/main/java/.../repository/WishlistRepository.java` | Wishlist data access    |
| `order-service/src/main/java/.../config/MongoConfig.java`            | Index initialization    |

### Modified Files

| File Path                                               | Changes                            |
| ------------------------------------------------------- | ---------------------------------- |
| `pom.xml` (root)                                        | Added order-service module         |
| `product-service/.../model/Product.java`                | Added text indexes, category, tags |
| `product-service/.../repository/ProductRepository.java` | Added search/filter/tag methods    |

---

## Questions for Team Discussion

1. **Cart in order-service vs user-service?**
   - Currently in order-service (closer to checkout flow)

2. **Order notifications?**
   - Kafka events for status changes?

3. **Product category values?**
   - Free-text or predefined enum?

4. **Return window?**
   - How many days after DELIVERED can user return?

---

# 15. Complete File Changelog (Database Owner)

> **Summary:** All files created or modified by the Database Owner for schema, integrity, and team documentation.

## New Files Created

### order-service Module (21 files)

| #   | File Path                                                                                | Purpose                                            |
| --- | ---------------------------------------------------------------------------------------- | -------------------------------------------------- |
| 1   | `order-service/pom.xml`                                                                  | Maven build configuration with Spring Data MongoDB |
| 2   | `order-service/Dockerfile`                                                               | Docker container build file                        |
| 3   | `order-service/src/main/resources/application.properties`                                | MongoDB connection & service config                |
| 4   | `order-service/src/main/java/ax/gritlab/buy_01/order/OrderServiceApplication.java`       | Spring Boot main class                             |
| 5   | `order-service/src/main/java/ax/gritlab/buy_01/order/model/Order.java`                   | Order entity with 14 indexes                       |
| 6   | `order-service/src/main/java/ax/gritlab/buy_01/order/model/OrderItem.java`               | Embedded order item with snapshotted fields        |
| 7   | `order-service/src/main/java/ax/gritlab/buy_01/order/model/OrderStatus.java`             | 7-state status enum                                |
| 8   | `order-service/src/main/java/ax/gritlab/buy_01/order/model/OrderStatusHistory.java`      | Audit trail for status changes                     |
| 9   | `order-service/src/main/java/ax/gritlab/buy_01/order/model/Cart.java`                    | Shopping cart entity                               |
| 10  | `order-service/src/main/java/ax/gritlab/buy_01/order/model/CartItem.java`                | Embedded cart item                                 |
| 11  | `order-service/src/main/java/ax/gritlab/buy_01/order/model/CartStatus.java`              | Cart status enum (ACTIVE, PURCHASED, etc.)         |
| 12  | `order-service/src/main/java/ax/gritlab/buy_01/order/model/Wishlist.java`                | Wishlist entity (bonus feature)                    |
| 13  | `order-service/src/main/java/ax/gritlab/buy_01/order/model/WishlistItem.java`            | Embedded wishlist item                             |
| 14  | `order-service/src/main/java/ax/gritlab/buy_01/order/model/ShippingAddress.java`         | Embedded shipping address                          |
| 15  | `order-service/src/main/java/ax/gritlab/buy_01/order/model/Role.java`                    | User role enum                                     |
| 16  | `order-service/src/main/java/ax/gritlab/buy_01/order/repository/OrderRepository.java`    | 20+ optimized query methods                        |
| 17  | `order-service/src/main/java/ax/gritlab/buy_01/order/repository/CartRepository.java`     | Cart data access with abandoned cart queries       |
| 18  | `order-service/src/main/java/ax/gritlab/buy_01/order/repository/WishlistRepository.java` | Wishlist data access                               |
| 19  | `order-service/src/main/java/ax/gritlab/buy_01/order/config/MongoConfig.java`            | Programmatic index creation                        |
| 20  | `order-service/src/main/java/ax/gritlab/buy_01/order/config/SecurityConfig.java`         | Security configuration                             |
| 21  | `order-service/src/main/java/ax/gritlab/buy_01/order/controller/HealthController.java`   | Health check endpoint                              |

### Documentation Files (2 files)

| #   | File Path                 | Purpose                                     |
| --- | ------------------------- | ------------------------------------------- |
| 22  | `Database integration.md` | Consolidated team documentation (this file) |
| 23  | `.env.example`            | Environment variable template for team      |

---

## Modified Files

### Root Project (1 file)

| #   | File Path | Changes Made                                           |
| --- | --------- | ------------------------------------------------------ |
| 1   | `pom.xml` | Added `<module>order-service</module>` to modules list |

### product-service Module (2 files)

| #   | File Path                                                                                   | Changes Made                                                                                     |
| --- | ------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| 2   | `product-service/src/main/java/ax/gritlab/buy_01/product/model/Product.java`                | Added `@TextIndexed` on `name`/`description`, added `category` field, added `tags[]` array field |
| 3   | `product-service/src/main/java/ax/gritlab/buy_01/product/repository/ProductRepository.java` | Added text search methods, price range queries, category filtering, tag-based queries            |

### Configuration (1 file)

| #   | File Path    | Changes Made                                      |
| --- | ------------ | ------------------------------------------------- |
| 4   | `.gitignore` | Added `mongodb-atlas-cred.txt` to secrets section |

---
