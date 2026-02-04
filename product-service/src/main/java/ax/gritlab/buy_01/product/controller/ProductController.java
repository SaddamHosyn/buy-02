package ax.gritlab.buy_01.product.controller;

import ax.gritlab.buy_01.product.dto.ProductRequest;
import ax.gritlab.buy_01.product.dto.ProductResponse;
import ax.gritlab.buy_01.product.dto.ProductSearchRequest;
import ax.gritlab.buy_01.product.dto.ProductSearchResponse;
import ax.gritlab.buy_01.product.model.User;
import ax.gritlab.buy_01.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    // ==================== Search & Filter Endpoints ====================

    /**
     * Search products with advanced filtering.
     * 
     * Query parameters:
     * - q: Text search (searches name and description)
     * - category: Filter by category
     * - minPrice: Minimum price
     * - maxPrice: Maximum price
     * - tags: Filter by tags (comma-separated, matches any)
     * - inStock: If true, only return products with quantity > 0
     * - sellerId: Filter by seller
     * - page: Page number (0-indexed)
     * - size: Page size (default 20)
     * - sort: Sort field (default createdAt)
     * - direction: Sort direction (asc or desc, default desc)
     */
    @GetMapping("/search")
    public ResponseEntity<ProductSearchResponse> searchProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) String sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        ProductSearchRequest request = ProductSearchRequest.builder()
                .q(q)
                .category(category)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .tags(tags)
                .inStock(inStock)
                .sellerId(sellerId)
                .build();

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) 
                ? Sort.Direction.ASC 
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        return ResponseEntity.ok(productService.searchProducts(request, pageable));
    }

    /**
     * Get all available categories.
     */
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        return ResponseEntity.ok(productService.getAllCategories());
    }

    /**
     * Get all available tags.
     */
    @GetMapping("/tags")
    public ResponseEntity<Set<String>> getAllTags() {
        return ResponseEntity.ok(productService.getAllTags());
    }

    /**
     * Get products by seller with pagination.
     */
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<ProductSearchResponse> getProductsBySeller(
            @PathVariable String sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) 
                ? Sort.Direction.ASC 
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        return ResponseEntity.ok(productService.getProductsBySeller(sellerId, pageable));
    }

    // ==================== CRUD Endpoints ====================

    @PostMapping
    @PreAuthorize("hasAuthority('SELLER')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request,
            Authentication authentication) {
        String userId = ((User) authentication.getPrincipal()).getId();
        ProductResponse createdProduct = productService.createProduct(request, userId);
        return ResponseEntity.ok(createdProduct);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SELLER')")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable String id, @Valid @RequestBody ProductRequest request,
            Authentication authentication) {
        String userId = ((User) authentication.getPrincipal()).getId();
        ProductResponse updatedProduct = productService.updateProduct(id, request, userId);
        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SELLER')")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id, Authentication authentication) {
        String userId = ((User) authentication.getPrincipal()).getId();
        productService.deleteProduct(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{productId}/media/{mediaId}")
    @PreAuthorize("hasAuthority('SELLER')")
    public ResponseEntity<ProductResponse> associateMedia(@PathVariable String productId, @PathVariable String mediaId,
            Authentication authentication) {
        String userId = ((User) authentication.getPrincipal()).getId();
        ProductResponse updatedProduct = productService.associateMedia(productId, mediaId, userId);
        return ResponseEntity.ok(updatedProduct);
    }

    /**
     * Remove media ID from product's mediaIds array
     * Called by Media Service when media is deleted
     */
    @DeleteMapping("/{productId}/remove-media/{mediaId}")
    public ResponseEntity<Void> removeMediaFromProduct(
            @PathVariable String productId,
            @PathVariable String mediaId) {
        productService.removeMediaFromProduct(productId, mediaId);
        return ResponseEntity.ok().build();
    }

    /**
     * Clean up all orphaned media IDs from products
     * This removes media IDs that no longer exist in the media database
     */
    @PostMapping("/cleanup-orphaned-media")
    public ResponseEntity<String> cleanupOrphanedMedia() {
        String result = productService.cleanupOrphanedMedia();
        return ResponseEntity.ok(result);
    }
}
