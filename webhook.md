# Webhook Endpoint Implementation Log

## Date: 2026-02-06

### Added GitHub Webhook Endpoint

- **Service:** api-gateway
- **Location:** [api-gateway/src/main/java/ax/gritlab/buy_01/apigateway/webhooks/GitHubWebhookController.java](api-gateway/src/main/java/ax/gritlab/buy_01/apigateway/webhooks/GitHubWebhookController.java)
- **Endpoint:** POST `/webhooks/github`
- **Description:** Handles GitHub webhook events. Logs the payload and responds with "Webhook received".
- **Controller:** `GitHubWebhookController`

#### Example Implementation:

```java
@RestController
@RequestMapping("/webhooks")
public class GitHubWebhookController {
    @PostMapping("/github")
    public ResponseEntity<String> handleGitHubWebhook(
            @RequestHeader("X-Hub-Signature-256") String signature,
            @RequestBody String payload) {
        // Process the webhook payload
        System.out.println("Received GitHub webhook: " + payload);
        return ResponseEntity.ok("Webhook received");
    }
}
```

---

## What Your Teammate Needs To Do

Your teammate needs to run their own tunnel on their machine. Share these steps with them:

Quick setup for your teammate:

```bash
# 1. Install cloudflared
brew install cloudflared          # macOS
# or: sudo apt install cloudflared  # Linux

# 2. Start the API gateway on port 8095
cd /path/to/buy-02/api-gateway
SERVER_PORT=8095 mvn spring-boot:run

# 3. Start the tunnel (in a new terminal)
cloudflared tunnel --url http://localhost:8095

# 4. Copy the new trycloudflare.com URL

# 5. Update the webhook in GitHub repo → Settings → Webhooks
#    Change the Payload URL to the new trycloudflare.com URL
```

Important notes for them:

Since they're a collaborator, they have access to Settings → Webhooks and can update the Payload URL to their own tunnel URL.

Every time the tunnel restarts, the URL changes, so the webhook needs updating in GitHub.

They need both the API gateway and cloudflared running simultaneously.
