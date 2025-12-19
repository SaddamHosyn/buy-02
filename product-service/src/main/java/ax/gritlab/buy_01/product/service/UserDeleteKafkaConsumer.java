package ax.gritlab.buy_01.product.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserDeleteKafkaConsumer {
    private final ProductService productService;

    @KafkaListener(topics = "user.deleted", groupId = "product-service-group")
    public void consumeUserDeleted(String userId) {
        productService.deleteProductsByUserId(userId);
    }
}