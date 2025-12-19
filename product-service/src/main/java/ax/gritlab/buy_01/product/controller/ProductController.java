package ax.gritlab.buy_01.product.controller;

import ax.gritlab.buy_01.product.dto.ProductRequest;
import ax.gritlab.buy_01.product.dto.ProductResponse;
import ax.gritlab.buy_01.product.model.User;
import ax.gritlab.buy_01.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
