package ax.gritlab.buy_01.product.service;

import ax.gritlab.buy_01.product.dto.ProductRequest;
import ax.gritlab.buy_01.product.dto.ProductResponse;
import ax.gritlab.buy_01.product.dto.StockUpdateRequest;
import ax.gritlab.buy_01.product.dto.StockUpdateResponse;
import ax.gritlab.buy_01.product.exception.ResourceNotFoundException;
import ax.gritlab.buy_01.product.exception.UnauthorizedException;
import ax.gritlab.buy_01.product.model.Product;
import ax.gritlab.buy_01.product.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private ProductRequest testProductRequest;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId("prod123");
        testProduct.setName("Test Product");
        testProduct.setDescription("A test product");
        testProduct.setPrice(99.99);
        testProduct.setQuantity(10);
        testProduct.setUserId("user123");
        testProduct.setCreatedAt(LocalDateTime.now());
        testProduct.setUpdatedAt(LocalDateTime.now());

        testProductRequest = new ProductRequest();
        testProductRequest.setName("Updated Product");
        testProductRequest.setDescription("Updated description");
        testProductRequest.setPrice(149.99);
        testProductRequest.setQuantity(20);
    }

    @Test
    @DisplayName("Should retrieve all products")
    void testGetAllProducts() {
        // Arrange
        List<Product> products = new ArrayList<>();
        products.add(testProduct);
        when(productRepository.findAll()).thenReturn(products);

        // Act
        List<ProductResponse> responses = productService.getAllProducts();

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should retrieve product by ID successfully")
    void testGetProductById() {
        // Arrange
        when(productRepository.findById("prod123")).thenReturn(Optional.of(testProduct));

        // Act
        ProductResponse response = productService.getProductById("prod123");

        // Assert
        assertNotNull(response);
        assertEquals("prod123", response.getId());
        assertEquals("Test Product", response.getName());
        verify(productRepository, times(1)).findById("prod123");
    }

    @Test
    @DisplayName("Should throw exception when product not found")
    void testGetProductByIdNotFound() {
        // Arrange
        when(productRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> productService.getProductById("nonexistent"));
        verify(productRepository, times(1)).findById("nonexistent");
    }

    @Test
    @DisplayName("Should create product successfully")
    void testCreateProduct() {
        // Arrange
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // Act
        ProductResponse response = productService.createProduct(testProductRequest, "user123");

        // Assert
        assertNotNull(response);
        assertEquals("Test Product", response.getName());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Should update product successfully")
    void testUpdateProduct() {
        // Arrange
        when(productRepository.findById("prod123")).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // Act
        ProductResponse response = productService.updateProduct("prod123", testProductRequest, "user123");

        // Assert
        assertNotNull(response);
        verify(productRepository, times(1)).findById("prod123");
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw exception when updating product with unauthorized user")
    void testUpdateProductUnauthorized() {
        // Arrange
        when(productRepository.findById("prod123")).thenReturn(Optional.of(testProduct));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> productService.updateProduct("prod123", testProductRequest, "differentUser"));
        verify(productRepository, times(1)).findById("prod123");
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should delete product successfully")
    void testDeleteProduct() {
        // Arrange
        testProduct.setMediaIds(new ArrayList<>());
        when(productRepository.findById("prod123")).thenReturn(Optional.of(testProduct));

        // Act
        productService.deleteProduct("prod123", "user123");

        // Assert
        verify(productRepository, times(1)).findById("prod123");
        verify(productRepository, times(1)).delete(testProduct);
    }

    @Test
    @DisplayName("Should throw exception when deleting product with unauthorized user")
    void testDeleteProductUnauthorized() {
        // Arrange
        when(productRepository.findById("prod123")).thenReturn(Optional.of(testProduct));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> productService.deleteProduct("prod123", "differentUser"));
        verify(productRepository, times(1)).findById("prod123");
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    @DisplayName("Should delete all products for a user")
    void testDeleteProductsByUserId() {
        // Arrange
        List<Product> userProducts = new ArrayList<>();
        userProducts.add(testProduct);
        when(productRepository.findByUserId("user123")).thenReturn(userProducts);

        // Act
        productService.deleteProductsByUserId("user123");

        // Assert
        verify(productRepository, times(1)).findByUserId("user123");
        verify(productRepository, times(1)).delete(testProduct);
    }

    // ===================== Stock Update Tests =====================

    @Nested
    @DisplayName("Decrement Stock Tests")
    class DecrementStockTests {

        @Test
        @DisplayName("Should decrement stock successfully for single product")
        void decrementStock_SingleProduct_Success() {
            when(productRepository.findById("prod123")).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);

            StockUpdateRequest request = StockUpdateRequest.builder()
                    .items(List.of(StockUpdateRequest.StockUpdateItem.builder()
                            .productId("prod123")
                            .quantity(3)
                            .build()))
                    .build();

            StockUpdateResponse response = productService.decrementStock(request);

            assertTrue(response.isSuccess());
            assertEquals("All stock updates successful", response.getMessage());
            assertEquals(1, response.getResults().size());
            StockUpdateResponse.StockUpdateResult result = response.getResults().get(0);
            assertTrue(result.isSuccess());
            assertEquals("prod123", result.getProductId());
            assertEquals("Test Product", result.getProductName());
            assertEquals(10, result.getPreviousStock());
            assertEquals(7, result.getNewStock());
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("Should decrement stock to zero successfully")
        void decrementStock_ToZero_Success() {
            testProduct.setQuantity(5);
            when(productRepository.findById("prod123")).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);

            StockUpdateRequest request = StockUpdateRequest.builder()
                    .items(List.of(StockUpdateRequest.StockUpdateItem.builder()
                            .productId("prod123")
                            .quantity(5)
                            .build()))
                    .build();

            StockUpdateResponse response = productService.decrementStock(request);

            assertTrue(response.isSuccess());
            StockUpdateResponse.StockUpdateResult result = response.getResults().get(0);
            assertTrue(result.isSuccess());
            assertEquals(5, result.getPreviousStock());
            assertEquals(0, result.getNewStock());
        }

        @Test
        @DisplayName("Should fail when insufficient stock")
        void decrementStock_InsufficientStock_Fails() {
            testProduct.setQuantity(2);
            when(productRepository.findById("prod123")).thenReturn(Optional.of(testProduct));

            StockUpdateRequest request = StockUpdateRequest.builder()
                    .items(List.of(StockUpdateRequest.StockUpdateItem.builder()
                            .productId("prod123")
                            .quantity(5)
                            .build()))
                    .build();

            StockUpdateResponse response = productService.decrementStock(request);

            assertFalse(response.isSuccess());
            assertEquals("Some stock updates failed", response.getMessage());
            StockUpdateResponse.StockUpdateResult result = response.getResults().get(0);
            assertFalse(result.isSuccess());
            assertTrue(result.getError().contains("Insufficient stock"));
            assertTrue(result.getError().contains("Available: 2"));
            assertTrue(result.getError().contains("Requested: 5"));
            assertEquals(2, result.getPreviousStock());
            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("Should fail when product not found")
        void decrementStock_ProductNotFound_Fails() {
            when(productRepository.findById("nonexistent")).thenReturn(Optional.empty());

            StockUpdateRequest request = StockUpdateRequest.builder()
                    .items(List.of(StockUpdateRequest.StockUpdateItem.builder()
                            .productId("nonexistent")
                            .quantity(1)
                            .build()))
                    .build();

            StockUpdateResponse response = productService.decrementStock(request);

            assertFalse(response.isSuccess());
            StockUpdateResponse.StockUpdateResult result = response.getResults().get(0);
            assertFalse(result.isSuccess());
            assertEquals("Product not found", result.getError());
            assertEquals("nonexistent", result.getProductId());
        }

        @Test
        @DisplayName("Should handle multiple products with mixed results")
        void decrementStock_MultipleProducts_MixedResults() {
            Product product2 = new Product();
            product2.setId("prod456");
            product2.setName("Product 2");
            product2.setQuantity(1);

            when(productRepository.findById("prod123")).thenReturn(Optional.of(testProduct));
            when(productRepository.findById("prod456")).thenReturn(Optional.of(product2));
            when(productRepository.findById("missing")).thenReturn(Optional.empty());
            when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

            StockUpdateRequest request = StockUpdateRequest.builder()
                    .items(List.of(
                            StockUpdateRequest.StockUpdateItem.builder()
                                    .productId("prod123").quantity(3).build(),
                            StockUpdateRequest.StockUpdateItem.builder()
                                    .productId("missing").quantity(1).build(),
                            StockUpdateRequest.StockUpdateItem.builder()
                                    .productId("prod456").quantity(5).build()))
                    .build();

            StockUpdateResponse response = productService.decrementStock(request);

            assertFalse(response.isSuccess());
            assertEquals(3, response.getResults().size());
            assertTrue(response.getResults().get(0).isSuccess());
            assertEquals(7, response.getResults().get(0).getNewStock());
            assertFalse(response.getResults().get(1).isSuccess());
            assertEquals("Product not found", response.getResults().get(1).getError());
            assertFalse(response.getResults().get(2).isSuccess());
            assertTrue(response.getResults().get(2).getError().contains("Insufficient stock"));
        }

        @Test
        @DisplayName("Should handle repository exception gracefully")
        void decrementStock_RepositoryException_HandledGracefully() {
            when(productRepository.findById("prod123")).thenThrow(new RuntimeException("DB error"));

            StockUpdateRequest request = StockUpdateRequest.builder()
                    .items(List.of(StockUpdateRequest.StockUpdateItem.builder()
                            .productId("prod123").quantity(1).build()))
                    .build();

            StockUpdateResponse response = productService.decrementStock(request);

            assertFalse(response.isSuccess());
            StockUpdateResponse.StockUpdateResult result = response.getResults().get(0);
            assertFalse(result.isSuccess());
            assertTrue(result.getError().contains("DB error"));
        }
    }

    @Nested
    @DisplayName("Increment Stock Tests")
    class IncrementStockTests {

        @Test
        @DisplayName("Should increment stock successfully")
        void incrementStock_Success() {
            when(productRepository.findById("prod123")).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);

            StockUpdateRequest request = StockUpdateRequest.builder()
                    .items(List.of(StockUpdateRequest.StockUpdateItem.builder()
                            .productId("prod123").quantity(5).build()))
                    .build();

            StockUpdateResponse response = productService.incrementStock(request);

            assertTrue(response.isSuccess());
            assertEquals("All stock restored successfully", response.getMessage());
            assertEquals(1, response.getResults().size());
            StockUpdateResponse.StockUpdateResult result = response.getResults().get(0);
            assertTrue(result.isSuccess());
            assertEquals("prod123", result.getProductId());
            assertEquals("Test Product", result.getProductName());
            assertEquals(10, result.getPreviousStock());
            assertEquals(15, result.getNewStock());
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("Should increment stock from zero")
        void incrementStock_FromZero_Success() {
            testProduct.setQuantity(0);
            when(productRepository.findById("prod123")).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);

            StockUpdateRequest request = StockUpdateRequest.builder()
                    .items(List.of(StockUpdateRequest.StockUpdateItem.builder()
                            .productId("prod123").quantity(10).build()))
                    .build();

            StockUpdateResponse response = productService.incrementStock(request);

            assertTrue(response.isSuccess());
            StockUpdateResponse.StockUpdateResult result = response.getResults().get(0);
            assertEquals(0, result.getPreviousStock());
            assertEquals(10, result.getNewStock());
        }

        @Test
        @DisplayName("Should fail when product not found on increment")
        void incrementStock_ProductNotFound_Fails() {
            when(productRepository.findById("nonexistent")).thenReturn(Optional.empty());

            StockUpdateRequest request = StockUpdateRequest.builder()
                    .items(List.of(StockUpdateRequest.StockUpdateItem.builder()
                            .productId("nonexistent").quantity(5).build()))
                    .build();

            StockUpdateResponse response = productService.incrementStock(request);

            assertFalse(response.isSuccess());
            assertEquals("Some stock restorations failed", response.getMessage());
            StockUpdateResponse.StockUpdateResult result = response.getResults().get(0);
            assertFalse(result.isSuccess());
            assertEquals("Product not found", result.getError());
        }

        @Test
        @DisplayName("Should increment stock for multiple products")
        void incrementStock_MultipleProducts_AllSuccess() {
            Product product2 = new Product();
            product2.setId("prod456");
            product2.setName("Product 2");
            product2.setQuantity(5);

            when(productRepository.findById("prod123")).thenReturn(Optional.of(testProduct));
            when(productRepository.findById("prod456")).thenReturn(Optional.of(product2));
            when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

            StockUpdateRequest request = StockUpdateRequest.builder()
                    .items(List.of(
                            StockUpdateRequest.StockUpdateItem.builder()
                                    .productId("prod123").quantity(2).build(),
                            StockUpdateRequest.StockUpdateItem.builder()
                                    .productId("prod456").quantity(3).build()))
                    .build();

            StockUpdateResponse response = productService.incrementStock(request);

            assertTrue(response.isSuccess());
            assertEquals(2, response.getResults().size());
            assertEquals(12, response.getResults().get(0).getNewStock());
            assertEquals(8, response.getResults().get(1).getNewStock());
            verify(productRepository, times(2)).save(any(Product.class));
        }

        @Test
        @DisplayName("Should handle repository exception on increment")
        void incrementStock_RepositoryException_HandledGracefully() {
            when(productRepository.findById("prod123")).thenThrow(new RuntimeException("Timeout"));

            StockUpdateRequest request = StockUpdateRequest.builder()
                    .items(List.of(StockUpdateRequest.StockUpdateItem.builder()
                            .productId("prod123").quantity(1).build()))
                    .build();

            StockUpdateResponse response = productService.incrementStock(request);

            assertFalse(response.isSuccess());
            StockUpdateResponse.StockUpdateResult result = response.getResults().get(0);
            assertFalse(result.isSuccess());
            assertTrue(result.getError().contains("Timeout"));
        }
    }
}
