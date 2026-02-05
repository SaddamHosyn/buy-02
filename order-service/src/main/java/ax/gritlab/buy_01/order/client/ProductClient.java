package ax.gritlab.buy_01.order.client;

import ax.gritlab.buy_01.order.dto.ProductDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class ProductClient {

    private final RestTemplate restTemplate;

    public ProductDTO getProductById(String productId) {
        return restTemplate.getForObject("http://product-service/products/" + productId, ProductDTO.class);
    }
}
