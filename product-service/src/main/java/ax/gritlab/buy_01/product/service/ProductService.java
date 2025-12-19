package ax.gritlab.buy_01.product.service;

import ax.gritlab.buy_01.product.dto.ProductRequest;
import ax.gritlab.buy_01.product.dto.ProductResponse;
import ax.gritlab.buy_01.product.exception.ResourceNotFoundException;
import ax.gritlab.buy_01.product.exception.UnauthorizedException;
import ax.gritlab.buy_01.product.model.Product;
import ax.gritlab.buy_01.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

@Service
@RequiredArgsConstructor
public class ProductService {
    // Delete all products for a user and publish product.deleted events
    public void deleteProductsByUserId(String userId) {
        List<Product> products = productRepository.findByUserId(userId);
        for (Product product : products) {
            List<String> mediaIds = product.getMediaIds();
            productRepository.delete(product);
            try {
                ObjectNode node = objectMapper.createObjectNode();
                node.put("id", product.getId());
                ArrayNode arr = node.putArray("mediaIds");
                if (mediaIds != null) {
                    for (String m : mediaIds) arr.add(m);
                }
                kafkaTemplate.send("product.deleted", objectMapper.writeValueAsString(node));
            } catch (Exception e) {
                kafkaTemplate.send("product.deleted", product.getId());
            }
        }
    }

    private final ProductRepository productRepository;
    private final RestTemplate restTemplate;
    private final org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${media.service.url:http://media-service:8083/media}")
    private String mediaServiceUrl;

    @Value("${media.public.url:https://localhost:8443/api/media}")
    private String mediaPublicUrl;

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::toProductResponse)
                .collect(Collectors.toList());
    }

    public ProductResponse getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return toProductResponse(product);
    }

    public ProductResponse createProduct(ProductRequest request, String userId) {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .userId(userId)
                .createdAt(now.toLocalDateTime())
                .updatedAt(now.toLocalDateTime())
                .build();
        Product saved = productRepository.save(product);
        return toProductResponse(saved);
    }

    public ProductResponse updateProduct(String id, ProductRequest request, String userId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        if (!product.getUserId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to update this product");
        }
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        product.setUpdatedAt(ZonedDateTime.now(ZoneOffset.UTC).toLocalDateTime());
        Product saved = productRepository.save(product);
        return toProductResponse(saved);
    }

    public void deleteProduct(String id, String userId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        if (!product.getUserId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to delete this product");
        }
        List<String> mediaIds = product.getMediaIds();
        productRepository.delete(product);
        // Publish Kafka event for product deletion
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("id", id);
            ArrayNode arr = node.putArray("mediaIds");
            if (mediaIds != null) {
                for (String m : mediaIds) arr.add(m);
            }
            kafkaTemplate.send("product.deleted", objectMapper.writeValueAsString(node));
        } catch (Exception e) {
            kafkaTemplate.send("product.deleted", id);
        }
    }

    public ProductResponse associateMedia(String productId, String mediaId, String userId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        if (!product.getUserId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to modify this product");
        }
        product.getMediaIds().add(mediaId);
        Product saved = productRepository.save(product);

        // Call Media Service to update the productId in the media record
        try {
            String url = mediaServiceUrl + "/images/" + mediaId + "/product/" + productId + "?userId=" + userId;
            restTemplate.put(url, null);
        } catch (Exception e) {
            // Log the error but don't fail the product update
            System.err.println("Failed to update media productId: " + e.getMessage());
        }

        return toProductResponse(saved);
    }

    /**
     * Remove media ID from product's mediaIds array
     * Called by Media Service when media is deleted
     */
    public void removeMediaFromProduct(String productId, String mediaId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.getMediaIds().remove(mediaId);
        productRepository.save(product);
    }

    /**
     * Clean up all orphaned media IDs from products
     * This removes media IDs that no longer exist in the media database
     */
    public String cleanupOrphanedMedia() {
        List<Product> products = productRepository.findAll();
        int totalCleaned = 0;

        for (Product product : products) {
            List<String> validMediaIds = new ArrayList<>();

            // Check each media ID to see if it still exists
            for (String mediaId : product.getMediaIds()) {
                try {
                    // Try to call media service to check if media exists
                    String url = mediaServiceUrl + "/images/" + mediaId;
                    restTemplate.headForHeaders(url);
                    // If no exception, media exists
                    validMediaIds.add(mediaId);
                } catch (org.springframework.web.client.HttpClientErrorException e) {
                    // Media doesn't exist (404) or forbidden (403) - remove it
                    if (e.getStatusCode().value() == 404 || e.getStatusCode().value() == 403) {
                        System.out.println("Removing orphaned/inaccessible media ID: " + mediaId + " from product: "
                                + product.getId());
                        totalCleaned++;
                    } else {
                        // Other error - keep the media ID
                        validMediaIds.add(mediaId);
                    }
                } catch (Exception e) {
                    // Unknown error - keep the media ID to be safe
                    validMediaIds.add(mediaId);
                }
            }

            // Update product if any media IDs were removed
            if (validMediaIds.size() != product.getMediaIds().size()) {
                int removedCount = product.getMediaIds().size() - validMediaIds.size();
                product.setMediaIds(validMediaIds);
                productRepository.save(product);
                System.out.println(
                        "Cleaned product: " + product.getId() + " - Removed " + removedCount + " orphaned media IDs");
            }
        }

        return "Cleaned up " + totalCleaned + " orphaned media references from products";
    }

    /**
     * Convert Product entity to ProductResponse DTO with imageUrls
     */
    private ProductResponse toProductResponse(Product product) {
        // Convert mediaIds to image URLs using public URL for browser access
        List<String> imageUrls = product.getMediaIds().stream()
                .map(mediaId -> mediaPublicUrl + "/images/" + mediaId)
                .collect(Collectors.toList());

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getQuantity())
                .sellerId(product.getUserId())
                .mediaIds(product.getMediaIds())
                .imageUrls(imageUrls)
                .createdAt(product.getCreatedAt() != null ? product.getCreatedAt().atZone(ZoneOffset.UTC).toString()
                        : null)
                .updatedAt(product.getUpdatedAt() != null ? product.getUpdatedAt().atZone(ZoneOffset.UTC).toString()
                        : null)
                .build();
    }
}
