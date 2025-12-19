package ax.gritlab.buy_01.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableMongoAuditing
public class MongoConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
