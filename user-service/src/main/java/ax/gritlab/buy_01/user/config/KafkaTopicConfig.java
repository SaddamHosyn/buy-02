package ax.gritlab.buy_01.user.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
   @Bean
   public NewTopic userDeletedTopic() {
      return TopicBuilder.name("user.deleted")
            .partitions(1)
            .replicas(1)
            .build();
   }
}
