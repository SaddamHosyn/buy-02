package ax.gritlab.buy_01.product.service;

import ax.gritlab.buy_01.product.dto.ProductRequest;
import ax.gritlab.buy_01.product.dto.ProductResponse;
import ax.gritlab.buy_01.product.dto.ProductSearchRequest;
import ax.gritlab.buy_01.product.dto.ProductSearchResponse;
import ax.gritlab.buy_01.product.exception.ResourceNotFoundException;
import ax.gritlab.buy_01.product.exception.UnauthorizedException;
import ax.gritlab.buy_01.product.model.Product;
import ax.gritlab.buy_01.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import ax.gritlab.buy_01.product.dto.PagedResponse;

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
                    for (String m : mediaIds)
                        arr.add(m);
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
    private final MongoTemplate mongoTemplate;

    @Value("${media.service.url:http://media-service:8083/media}")
    private String mediaServiceUrl;

    @Value("${media.public.url:https://localhost:8443/api/media}")
    private String mediaPublicUrl;

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::toProductResponse)
                .collect(Collectors.toList());
    }

    public PagedResponse<ProductResponse> searchProducts(
            String keyword,
            String category,
            Double minPrice,
            Double maxPrice,
            Pageable pageable) {

        Query query = new Query().with(pageable);
        List<Criteria> criteria = new ArrayList<>();

        if (keyword != null && !keyword.isEmpty()) {
            criteria.add(new Criteria().orOperator(
                    Criteria.where("name").regex(keyword, "i"),
                    Criteria.where("description").regex(keyword, "i")));
        }

        if (category != null && !category.isEmpty()) {
            criteria.add(Criteria.where("category").is(category));
        }

        if (minPrice != null || maxPrice != null) {
            Criteria priceCriteria = Criteria.where("price");
            if (minPrice != null)
                priceCriteria.gte(minPrice);
            if (maxPrice != null)
                priceCriteria.lte(maxPrice);
            criteria.add(priceCriteria);
        }

        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(query.skip(-1).limit(-1), Product.class);
        List<Product> products = mongoTemplate.find(query, Product.class);

        List<ProductResponse> content = products.stream()
                .map(this::toProductResponse)
                .collect(Collectors.toList());

        Page<ProductResponse> page = new PageImpl<>(content, pageable, total);

        return PagedResponse.<ProductResponse>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    public ProductResponse getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return toProductResponse(product);
    }

    public List<String> getCategories() {
        return productRepository.findDistinctCategories().stream()
                .map(Product::getCategory)
                .filter(c -> c != null && !c.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    public ProductResponse createProduct(ProductRequest request, String userId) {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .category(request.getCategory())
                .tags(request.getTags() != null ? request.getTags() : new ArrayList<>())
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
        if (request.getCategory() != null) {
            product.setCategory(request.getCategory());
        }
        if (request.getTags() != null) {
            product.setTags(request.getTags());
        }
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
                for (String m : mediaIds)
                    arr.add(m);
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
                .category(product.getCategory())
                .tags(product.getTags())
                .mediaIds(product.getMediaIds())
                .imageUrls(imageUrls)
                .createdAt(product.getCreatedAt() != null ? product.getCreatedAt().atZone(ZoneOffset.UTC).toString()
                        : null)
                .updatedAt(product.getUpdatedAt() != null ? product.getUpdatedAt().atZone(ZoneOffset.UTC).toString()
                        : null)
                .build();
    }

    // ==================== Search & Filter Methods ====================

    /**
     * Search products with advanced filtering.
     * Supports text search, category, price range, tags, and stock filtering.
     */
    public ProductSearchResponse searchProducts(ProductSearchRequest request, Pageable pageable) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        // Text search (if q is provided)
        if (request.getQ() != null && !request.getQ().trim().isEmpty()) {
            // Use TextCriteria for full-text search on indexed fields
            TextCriteria textCriteria = TextCriteria.forDefaultLanguage()
                    .matching(request.getQ().trim());
            query = TextQuery.queryText(textCriteria).sortByScore();
        }

        // Category filter
        if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
            criteriaList.add(Criteria.where("category").is(request.getCategory().trim()));
        }

        // Price range filter
        if (request.getMinPrice() != null && request.getMaxPrice() != null) {
            criteriaList.add(Criteria.where("price").gte(request.getMinPrice()).lte(request.getMaxPrice()));
        } else if (request.getMinPrice() != null) {
            criteriaList.add(Criteria.where("price").gte(request.getMinPrice()));
        } else if (request.getMaxPrice() != null) {
            criteriaList.add(Criteria.where("price").lte(request.getMaxPrice()));
        }

        // Tags filter (match any)
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            criteriaList.add(Criteria.where("tags").in(request.getTags()));
        }

        // In-stock filter
        if (request.getInStock() != null && request.getInStock()) {
            criteriaList.add(Criteria.where("quantity").gt(0));
        }

        // Seller filter
        if (request.getSellerId() != null && !request.getSellerId().trim().isEmpty()) {
            criteriaList.add(Criteria.where("userId").is(request.getSellerId().trim()));
        }

        // Combine all criteria
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        // Execute count query for pagination
        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Product.class);

        // Apply pagination
        query.with(pageable);

        // Execute query
        List<Product> products = mongoTemplate.find(query, Product.class);

        // Convert to response
        List<ProductResponse> productResponses = products.stream()
                .map(this::toProductResponse)
                .collect(Collectors.toList());

        return ProductSearchResponse.builder()
                .products(productResponses)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(total)
                .totalPages((int) Math.ceil((double) total / pageable.getPageSize()))
                .first(pageable.getPageNumber() == 0)
                .last(pageable.getPageNumber() >= (int) Math.ceil((double) total / pageable.getPageSize()) - 1)
                .build();
    }

    /**
     * Get all distinct categories in the system.
     */
    public List<String> getAllCategories() {
        return mongoTemplate.query(Product.class)
                .distinct("category")
                .as(String.class)
                .all()
                .stream()
                .filter(cat -> cat != null && !cat.isEmpty())
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get all distinct tags in the system.
     */
    public Set<String> getAllTags() {
        return mongoTemplate.query(Product.class)
                .distinct("tags")
                .as(String.class)
                .all()
                .stream()
                .filter(tag -> tag != null && !tag.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Get products by seller with pagination.
     */
    public ProductSearchResponse getProductsBySeller(String sellerId, Pageable pageable) {
        Page<Product> page = productRepository.findByUserId(sellerId, pageable);
        
        List<ProductResponse> productResponses = page.getContent().stream()
                .map(this::toProductResponse)
                .collect(Collectors.toList());

        return ProductSearchResponse.builder()
                .products(productResponses)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
