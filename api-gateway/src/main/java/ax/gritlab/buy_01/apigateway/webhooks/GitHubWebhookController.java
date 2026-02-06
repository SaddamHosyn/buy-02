package ax.gritlab.buy_01.apigateway.webhooks;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/webhooks")
public class GitHubWebhookController {

    @PostMapping("/github")
    public Mono<ResponseEntity<String>> handleGitHubWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String payload) {

        System.out.println("Received GitHub webhook: " + payload);
        return Mono.just(ResponseEntity.ok("Webhook received"));
    }
}
