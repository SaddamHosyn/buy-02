package ax.gritlab.buy_01.order.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the authenticated user extracted from JWT token.
 * This is stored in the SecurityContext principal.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticatedUser {

   private String userId;
   private String email;
   private String role;

   /**
    * Check if user has SELLER role.
    */
   public boolean isSeller() {
      return "SELLER".equalsIgnoreCase(role);
   }

   /**
    * Check if user has CLIENT role.
    */
   public boolean isClient() {
      return "CLIENT".equalsIgnoreCase(role);
   }
}
