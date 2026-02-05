package ax.gritlab.buy_01.order.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT Authentication Filter that extracts user information from JWT tokens
 * and sets up the Spring Security context for authorization.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

   private final JwtService jwtService;

   @Override
   protected void doFilterInternal(
         @NonNull HttpServletRequest request,
         @NonNull HttpServletResponse response,
         @NonNull FilterChain filterChain) throws ServletException, IOException {

      final String authHeader = request.getHeader("Authorization");
      final String jwt;

      // Skip if no Authorization header or not a Bearer token
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
         filterChain.doFilter(request, response);
         return;
      }

      jwt = authHeader.substring(7);

      try {
         // Validate token and extract claims
         if (jwtService.isTokenValid(jwt)) {
            String userId = jwtService.extractUserId(jwt);
            String username = jwtService.extractUsername(jwt);
            String role = jwtService.extractRole(jwt);

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
               // Create authentication with user details
               AuthenticatedUser user = new AuthenticatedUser(userId, username, role);

               List<SimpleGrantedAuthority> authorities = List.of(
                     new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "CLIENT")));

               UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                     user,
                     null,
                     authorities);
               authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

               SecurityContextHolder.getContext().setAuthentication(authToken);

               log.debug("Authenticated user: {} with role: {}", userId, role);
            }
         }
      } catch (Exception e) {
         log.error("JWT validation failed: {}", e.getMessage());
         // Continue without authentication - will be rejected by security config if
         // needed
      }

      filterChain.doFilter(request, response);
   }
}
