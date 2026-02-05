# Buy-02 Frontend: Angular Implementation Guide

This document briefly explains the architecture and features of the Buy-02 frontend application.

## 🏗️ Core Architecture

The application is built with **Angular 17+** using **Standalone Components** and **Signals** for reactive state management. This eliminates `NgModules` and provides a more modern, performant developer experience.

---

## 🔐 Security & Authentication

### **Route Guards** (`/core/guards/`)

- **`authGuard`**: Protects routes like `/profile` and the seller dashboard. It checks `authService.isAuthenticated()` and redirects to `/login` if necessary, preserving the intended URL as a query parameter.
- **`roleGuard`**: Implements Role-Based Access Control (RBAC). It checks if the logged-in user has the specific role (e.g., `SELLER`) defined in the route data before allowing access.

### **HTTP Interceptors** (`/core/interceptors/`)

- **`authInterceptor`**: Automatically injects a `Bearer <token>` header into every outgoing HTTP request if a JWT is present in local storage.
- **`errorInterceptor`**: A global safety net.
  - **401 Errors**: Automatically clears the session and redirects to login.
  - **403 Errors**: Displays a "Permission Denied" notification.
  - **Networking**: Detects connectivity issues and alerts the user.

---

## 📊 Dashboards & Profiles (`/features/profile/` & `/features/seller/`)

The app features dual-mode dashboards powered by **ng2-charts**:

- **Buyer Dashboard**: Displays total lifetime spend, most frequently purchased items, and a pie chart of top categories.
- **Seller Dashboard**: Provides high-level business metrics including total revenue, units sold, and a bar chart of best-selling products.
- **Reactive State**: Dashboard data responds immediately to profile changes (like avatar updates) via Angular Signals.

---

## 🔍 Search & Discovery (`/features/products/product-list/`)

The product discovery engine includes:

- **Keyword Search**: Real-time filtering using case-insensitive regex on the backend.
- **Filtering System**: Integrated price range sliders and category selection that synchronize with the URL for shareable search results.
- **Sorting & Pagination**: Managed via signals to handle large datasets efficiently.

---

## 🛒 Cart & Checkout (Experimental)

_Features are currently in active development:_

- **Cart Logic**: Implemented but localized to session state. Supports quantity updates and subtotal calculations.
- **Checkout Wizard**: A multi-step Angular Material Stepper (`mat-stepper`) handling Address → Review → Confirmation flows.

---

## 🎨 UI/UX Implementation

- **Responsive Navigation**: The `Navbar` component handles transitions from a full desktop toolbar to a mobile hamburger menu using CSS media queries and `mat-menu`.
- **Feedback Loop**: Integrated `MatProgressBar` in `App.html` that reacts to Router events, providing visual feedback during asynchronous route loading.
- **Reactive Forms**: All user inputs use `ReactiveFormsModule` with custom validation logic (e.g., password matching, price validation).

---

## 🛠️ Testing the Flows

1. **Check Auth Interceptor**: Open Network Tab -> Inspect any API Headers for `Authorization`.
2. **Check Guards**: While logged out, try to navigate directly to `/seller/dashboard`.
3. **Check Layout**: Resize the browser to see the menu collapse and the product grid adapt.
