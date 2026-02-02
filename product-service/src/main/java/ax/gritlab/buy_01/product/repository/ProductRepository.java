package ax.gritlab.buy_01.product.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import ax.gritlab.buy_01.product.model.Product;

import java.util.List;

/**
 * Product repository with methods for:
 * - Basic CRUD
 * - Text search
 * - Filtering by price, category, stock
 * - Seller product queries
 */
public interface ProductRepository extends MongoRepository<Product, String> {

	// ==================== Seller Queries ====================

	/**
	 * Find all products by seller ID.
	 */
	List<Product> findByUserId(String userId);

	/**
	 * Find products by seller with pagination.
	 */
	Page<Product> findByUserId(String userId, Pageable pageable);

	/**
	 * Delete all products by seller.
	 */
	void deleteByUserId(String userId);

	/**
	 * Count products by seller.
	 */
	long countByUserId(String userId);

	// ==================== Text Search ====================

	/**
	 * Full-text search on name and description.
	 * Results sorted by text score (relevance).
	 */
	@Query("{ $text: { $search: ?0 } }")
	Page<Product> searchByText(String searchText, Pageable pageable);

	/**
	 * Text search with minimum score threshold.
	 */
	@Query("{ $text: { $search: ?0 }, score: { $meta: 'textScore' } }")
	List<Product> searchByTextWithScore(String searchText);

	// ==================== Price Filtering ====================

	/**
	 * Find products within price range.
	 */
	Page<Product> findByPriceBetween(Double minPrice, Double maxPrice, Pageable pageable);

	/**
	 * Find products under max price.
	 */
	Page<Product> findByPriceLessThanEqual(Double maxPrice, Pageable pageable);

	/**
	 * Find products above min price.
	 */
	Page<Product> findByPriceGreaterThanEqual(Double minPrice, Pageable pageable);

	// ==================== Category Filtering ====================

	/**
	 * Find products by category.
	 */
	Page<Product> findByCategory(String category, Pageable pageable);

	/**
	 * Find products by category within price range.
	 */
	@Query("{ 'category': ?0, 'price': { $gte: ?1, $lte: ?2 } }")
	Page<Product> findByCategoryAndPriceRange(String category, Double minPrice, Double maxPrice, Pageable pageable);

	/**
	 * Get distinct categories.
	 */
	@Query(value = "{}", fields = "{ 'category': 1 }")
	List<Product> findDistinctCategories();

	// ==================== Tag Filtering ====================

	/**
	 * Find products by tag.
	 */
	@Query("{ 'tags': ?0 }")
	Page<Product> findByTag(String tag, Pageable pageable);

	/**
	 * Find products matching any of the given tags.
	 */
	@Query("{ 'tags': { $in: ?0 } }")
	Page<Product> findByTagsIn(List<String> tags, Pageable pageable);

	/**
	 * Find products matching all of the given tags.
	 */
	@Query("{ 'tags': { $all: ?0 } }")
	Page<Product> findByTagsAll(List<String> tags, Pageable pageable);

	/**
	 * Find products by category and tag.
	 */
	@Query("{ 'category': ?0, 'tags': ?1 }")
	Page<Product> findByCategoryAndTag(String category, String tag, Pageable pageable);

	// ==================== Stock Filtering ====================

	/**
	 * Find in-stock products only.
	 */
	Page<Product> findByQuantityGreaterThan(Integer minQuantity, Pageable pageable);

	/**
	 * Find in-stock products in category.
	 */
	@Query("{ 'category': ?0, 'quantity': { $gt: 0 } }")
	Page<Product> findInStockByCategory(String category, Pageable pageable);

	// ==================== Combined Filters ====================

	/**
	 * Advanced filter: category + price range + in-stock.
	 */
	@Query("{ " +
			"$and: [ " +
			"  { $or: [ { 'category': { $exists: false } }, { 'category': ?0 }, { ?0: null } ] }, " +
			"  { 'price': { $gte: ?1, $lte: ?2 } }, " +
			"  { 'quantity': { $gt: 0 } } " +
			"] }")
	Page<Product> findWithFilters(String category, Double minPrice, Double maxPrice, Pageable pageable);

	/**
	 * Search with text + price filter + in-stock.
	 */
	@Query("{ " +
			"$text: { $search: ?0 }, " +
			"'price': { $gte: ?1, $lte: ?2 }, " +
			"'quantity': { $gt: 0 } " +
			"}")
	Page<Product> searchWithFilters(String searchText, Double minPrice, Double maxPrice, Pageable pageable);

	/**
	 * Advanced filter: category + tags + price range + in-stock.
	 */
	@Query("{ " +
			"$and: [ " +
			"  { $or: [ { 'category': null }, { 'category': ?0 } ] }, " +
			"  { $or: [ { ?1: null }, { 'tags': { $in: ?1 } } ] }, " +
			"  { 'price': { $gte: ?2, $lte: ?3 } }, " +
			"  { 'quantity': { $gt: 0 } } " +
			"] }")
	Page<Product> findWithAllFilters(String category, List<String> tags, Double minPrice, Double maxPrice,
			Pageable pageable);

	// ==================== Statistics ====================

	/**
	 * Find top products by a seller (for profile stats).
	 * Used with aggregation in service layer.
	 */
	@Query("{ 'userId': ?0 }")
	List<Product> findProductsBySellerForStats(String sellerId);
}
