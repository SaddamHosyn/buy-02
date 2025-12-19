package ax.gritlab.buy_01.product.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ax.gritlab.buy_01.product.model.Product;

public interface ProductRepository extends MongoRepository<Product, String> {
	void deleteByUserId(String userId);
	java.util.List<Product> findByUserId(String userId);
}
