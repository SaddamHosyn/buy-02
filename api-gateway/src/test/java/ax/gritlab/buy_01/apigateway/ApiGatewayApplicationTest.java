package ax.gritlab.buy_01.apigateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.profiles.active=test"}
)
@AutoConfigureWebTestClient
@DisplayName("API Gateway Integration Tests")
public class ApiGatewayApplicationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("Should start API Gateway application successfully")
    void testApplicationContextLoads() {
        assertNotNull(webTestClient);
    }

    @Test
    @DisplayName("Should return 404 for non-existent route")
    void testNonExistentRoute() {
        webTestClient.get()
                .uri("/api/nonexistent")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Should have gateway actuator available")
    void testActuatorEndpoint() {
        webTestClient.get()
                .uri("/actuator")
                .exchange()
                .expectStatus().isOk();
    }
}
