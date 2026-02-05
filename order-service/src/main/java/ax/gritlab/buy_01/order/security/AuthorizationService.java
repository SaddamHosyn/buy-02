package ax.gritlab.buy_01.order.security;

import ax.gritlab.buy_01.order.model.Order;
import ax.gritlab.buy_01.order.model.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

/**
 * Service for handling authorization checks for orders.
 * 
 * Implements three key authorization rules:
 * 1. User vs Seller capabilities - different roles can perform different
 * actions
 * 2. Order ownership - users can only access/modify their own orders
 * 3. Mutable status rules - orders can only be modified in certain states
 */
@Service
@Slf4j
public class AuthorizationService {

   // Status that allow cancellation
   private static final Set<OrderStatus> CANCELLABLE_STATUSES = Set.of(
         OrderStatus.PENDING,
         OrderStatus.CONFIRMED,
         OrderStatus.PROCESSING);

   // Status that allow removal (soft delete)
   private static final Set<OrderStatus> REMOVABLE_STATUSES = Set.of(
         OrderStatus.CANCELLED,
         OrderStatus.DELIVERED,
         OrderStatus.RETURNED);

   // Seller-only status transitions
   private static final Set<OrderStatus> SELLER_ONLY_TRANSITIONS = Set.of(
         OrderStatus.CONFIRMED,
         OrderStatus.PROCESSING,
         OrderStatus.SHIPPED);

   /**
    * Get the currently authenticated user from SecurityContext.
    * 
    * @return AuthenticatedUser or null if not authenticated
    */
   public AuthenticatedUser getCurrentUser() {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser) {
         return (AuthenticatedUser) auth.getPrincipal();
      }
      return null;
   }

   /**
    * Get current user or throw 401 Unauthorized.
    */
   public AuthenticatedUser requireAuthentication() {
      AuthenticatedUser user = getCurrentUser();
      if (user == null) {
         throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
      }
      return user;
   }

   /**
    * Validate that the authenticated user matches the requested userId.
    * This prevents users from accessing other users' data.
    * 
    * @param requestedUserId The user ID from the request path/body
    * @throws ResponseStatusException 403 if user doesn't match
    */
   public void validateUserOwnership(String requestedUserId) {
      AuthenticatedUser user = requireAuthentication();
      if (!user.getUserId().equals(requestedUserId)) {
         log.warn("User {} attempted to access data for user {}", user.getUserId(), requestedUserId);
         throw new ResponseStatusException(HttpStatus.FORBIDDEN,
               "You can only access your own data");
      }
   }

   /**
    * Validate that the authenticated user owns the order (is the buyer).
    * 
    * @param order The order to check ownership of
    * @throws ResponseStatusException 403 if user is not the buyer
    */
   public void validateOrderOwnership(Order order) {
      AuthenticatedUser user = requireAuthentication();
      if (!user.getUserId().equals(order.getBuyerId())) {
         log.warn("User {} attempted to modify order {} owned by {}",
               user.getUserId(), order.getId(), order.getBuyerId());
         throw new ResponseStatusException(HttpStatus.FORBIDDEN,
               "You can only modify your own orders");
      }
   }

   /**
    * Validate that the authenticated user is a seller with products in the order.
    * 
    * @param order The order to check
    * @throws ResponseStatusException 403 if user is not a seller in this order
    */
   public void validateSellerInOrder(Order order) {
      AuthenticatedUser user = requireAuthentication();
      if (!user.isSeller()) {
         throw new ResponseStatusException(HttpStatus.FORBIDDEN,
               "Only sellers can perform this action");
      }
      if (!order.getSellerIds().contains(user.getUserId())) {
         log.warn("Seller {} attempted to access order {} where they have no products",
               user.getUserId(), order.getId());
         throw new ResponseStatusException(HttpStatus.FORBIDDEN,
               "You can only access orders containing your products");
      }
   }

   /**
    * Validate that the user can access an order (either as buyer or seller).
    * 
    * @param order The order to check access to
    * @throws ResponseStatusException 403 if user has no access to this order
    */
   public void validateOrderAccess(Order order) {
      AuthenticatedUser user = requireAuthentication();
      boolean isBuyer = user.getUserId().equals(order.getBuyerId());
      boolean isSellerInOrder = order.getSellerIds().contains(user.getUserId());

      if (!isBuyer && !isSellerInOrder) {
         log.warn("User {} attempted to access order {} without permission",
               user.getUserId(), order.getId());
         throw new ResponseStatusException(HttpStatus.FORBIDDEN,
               "You do not have access to this order");
      }
   }

   /**
    * Validate that an order can be cancelled based on status rules.
    * 
    * @param order The order to check
    * @throws ResponseStatusException 400 if order cannot be cancelled
    */
   public void validateCanCancel(Order order) {
      if (!CANCELLABLE_STATUSES.contains(order.getStatus())) {
         throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
               String.format("Order cannot be cancelled. Current status: %s. " +
                     "Only orders with status PENDING, CONFIRMED, or PROCESSING can be cancelled.",
                     order.getStatus()));
      }
   }

   /**
    * Validate that an order can be redone.
    * 
    * @param order The order to check
    * @throws ResponseStatusException 400 if order cannot be redone
    */
   public void validateCanRedo(Order order) {
      if (order.getStatus() != OrderStatus.CANCELLED) {
         throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
               String.format("Only cancelled orders can be redone. Current status: %s",
                     order.getStatus()));
      }
   }

   /**
    * Validate that an order can be removed (soft delete).
    * 
    * @param order The order to check
    * @throws ResponseStatusException 400 if order cannot be removed
    */
   public void validateCanRemove(Order order) {
      if (!REMOVABLE_STATUSES.contains(order.getStatus())) {
         throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
               String.format("Order cannot be removed. Current status: %s. " +
                     "Only cancelled, delivered, or returned orders can be removed.",
                     order.getStatus()));
      }
   }

   /**
    * Validate that a status transition requires seller role.
    * Sellers can: CONFIRM, PROCESS, SHIP orders
    * 
    * @param newStatus The target status
    * @throws ResponseStatusException 403 if seller role is required but user is
    *                                 not a seller
    */
   public void validateSellerOnlyTransition(OrderStatus newStatus) {
      if (SELLER_ONLY_TRANSITIONS.contains(newStatus)) {
         AuthenticatedUser user = requireAuthentication();
         if (!user.isSeller()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                  String.format("Only sellers can transition orders to %s status", newStatus));
         }
      }
   }

   /**
    * Require that the current user is a seller.
    * 
    * @throws ResponseStatusException 403 if user is not a seller
    */
   public void requireSellerRole() {
      AuthenticatedUser user = requireAuthentication();
      if (!user.isSeller()) {
         throw new ResponseStatusException(HttpStatus.FORBIDDEN,
               "This action requires seller privileges");
      }
   }

   /**
    * Require that the current user is a client (buyer).
    * 
    * @throws ResponseStatusException 403 if user is not a client
    */
   public void requireClientRole() {
      AuthenticatedUser user = requireAuthentication();
      if (!user.isClient()) {
         throw new ResponseStatusException(HttpStatus.FORBIDDEN,
               "This action requires client privileges");
      }
   }

   /**
    * Get the role string for audit logging.
    */
   public String getCurrentUserRole() {
      AuthenticatedUser user = getCurrentUser();
      return user != null ? user.getRole() : "ANONYMOUS";
   }

   /**
    * Get the current user ID for audit logging.
    */
   public String getCurrentUserId() {
      AuthenticatedUser user = getCurrentUser();
      return user != null ? user.getUserId() : null;
   }
}
