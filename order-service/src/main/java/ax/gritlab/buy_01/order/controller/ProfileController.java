package ax.gritlab.buy_01.order.controller;

import ax.gritlab.buy_01.order.dto.BuyerProfileStatsDto;
import ax.gritlab.buy_01.order.dto.SellerProfileStatsDto;
import ax.gritlab.buy_01.order.model.Role;
import ax.gritlab.buy_01.order.model.User;
import ax.gritlab.buy_01.order.service.ProfileStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for profile statistics endpoints.
 * Provides stats for both buyers and sellers.
 */
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileStatsService profileStatsService;

    /**
     * Get current user's buyer statistics.
     * Available for all authenticated users (both CLIENT and SELLER can view their purchase history).
     */
    @GetMapping("/buyer/me")
    public ResponseEntity<BuyerProfileStatsDto> getMyBuyerStats(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        BuyerProfileStatsDto stats = profileStatsService.getBuyerStats(user.getId());
        return ResponseEntity.ok(stats);
    }

    /**
     * Get buyer statistics by user ID.
     * Restricted: users can only view their own stats, or sellers viewing buyers who purchased from them.
     */
    @GetMapping("/buyer/{userId}")
    public ResponseEntity<BuyerProfileStatsDto> getBuyerStats(
            @PathVariable String userId,
            Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        
        // Users can only view their own buyer stats
        if (!currentUser.getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        BuyerProfileStatsDto stats = profileStatsService.getBuyerStats(userId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get current user's seller statistics.
     * Only available for SELLER role.
     */
    @GetMapping("/seller/me")
    @PreAuthorize("hasAuthority('SELLER')")
    public ResponseEntity<SellerProfileStatsDto> getMySellerStats(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        SellerProfileStatsDto stats = profileStatsService.getSellerStats(user.getId());
        return ResponseEntity.ok(stats);
    }

    /**
     * Get seller statistics by seller ID.
     * Restricted: Only the seller themselves can view their detailed stats.
     */
    @GetMapping("/seller/{sellerId}")
    @PreAuthorize("hasAuthority('SELLER')")
    public ResponseEntity<SellerProfileStatsDto> getSellerStats(
            @PathVariable String sellerId,
            Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        
        // Sellers can only view their own seller stats
        if (!currentUser.getId().equals(sellerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        SellerProfileStatsDto stats = profileStatsService.getSellerStats(sellerId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get a public summary of seller performance.
     * This provides limited stats visible to all users (for product pages).
     */
    @GetMapping("/seller/{sellerId}/public")
    public ResponseEntity<SellerPublicStatsDto> getSellerPublicStats(@PathVariable String sellerId) {
        SellerProfileStatsDto fullStats = profileStatsService.getSellerStats(sellerId);
        
        // Return only public-safe stats
        SellerPublicStatsDto publicStats = SellerPublicStatsDto.builder()
                .sellerId(sellerId)
                .totalProductsSold(fullStats.getTotalProductsSold())
                .totalOrders(fullStats.getTotalOrders())
                .build();
        
        return ResponseEntity.ok(publicStats);
    }

    /**
     * Minimal public stats DTO for sellers.
     * Hides sensitive financial information.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SellerPublicStatsDto {
        private String sellerId;
        private Integer totalProductsSold;
        private Integer totalOrders;
    }
}
