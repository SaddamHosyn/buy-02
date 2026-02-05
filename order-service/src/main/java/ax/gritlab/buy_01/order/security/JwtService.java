package ax.gritlab.buy_01.order.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * JWT Service for extracting and validating JWT tokens.
 * Mirrors the user-service JwtService for consistent token handling.
 */
@Service
public class JwtService {

   @Value("${jwt.secret.key}")
   private String secretKey;

   /**
    * Extract username (email) from token.
    */
   public String extractUsername(String token) {
      return extractClaim(token, Claims::getSubject);
   }

   /**
    * Extract user ID from token claims.
    */
   public String extractUserId(String token) {
      Claims claims = extractAllClaims(token);
      return claims.get("userId", String.class);
   }

   /**
    * Extract user role from token claims.
    * Returns the role string (e.g., "SELLER", "CLIENT").
    */
   public String extractRole(String token) {
      Claims claims = extractAllClaims(token);
      // The authorities are stored as a list of GrantedAuthority objects
      Object authoritiesObj = claims.get("authorities");
      if (authoritiesObj instanceof java.util.List<?> authorities) {
         if (!authorities.isEmpty()) {
            Object first = authorities.get(0);
            if (first instanceof java.util.Map<?, ?> map) {
               Object authority = map.get("authority");
               if (authority != null) {
                  String role = authority.toString();
                  // Remove "ROLE_" prefix if present
                  return role.startsWith("ROLE_") ? role.substring(5) : role;
               }
            }
         }
      }
      return null;
   }

   /**
    * Check if token is valid (not expired).
    */
   public boolean isTokenValid(String token) {
      try {
         return !isTokenExpired(token);
      } catch (Exception e) {
         return false;
      }
   }

   /**
    * Extract a specific claim from token.
    */
   public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
      final Claims claims = extractAllClaims(token);
      return claimsResolver.apply(claims);
   }

   private boolean isTokenExpired(String token) {
      return extractExpiration(token).before(new Date());
   }

   private Date extractExpiration(String token) {
      return extractClaim(token, Claims::getExpiration);
   }

   private Claims extractAllClaims(String token) {
      return Jwts
            .parser()
            .verifyWith(getSignInKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
   }

   private SecretKey getSignInKey() {
      byte[] keyBytes = Decoders.BASE64.decode(secretKey);
      return Keys.hmacShaKeyFor(keyBytes);
   }
}
