package ax.gritlab.buy_01.media.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserDeleteKafkaConsumer {
    @Autowired
    private final MediaService mediaService;

    @KafkaListener(topics = "user.deleted", groupId = "media-service-group")
    public void consumeUserDeleted(String userId) {
        System.out.println("Received user deletion event for ID: " + userId);
        mediaService.deleteMediaByUserId(userId);
    }
}
