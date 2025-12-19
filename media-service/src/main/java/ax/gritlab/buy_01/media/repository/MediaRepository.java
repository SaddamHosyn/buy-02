package ax.gritlab.buy_01.media.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ax.gritlab.buy_01.media.model.Media;

import java.util.List;

public interface MediaRepository extends MongoRepository<Media, String> {
    List<Media> findByUserId(String userId);

    List<Media> findByProductId(String productId);

	void deleteByProductId(String productId);
}
