package ax.gritlab.buy_01.order.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuyerStats {
    private double totalSpent;
    private int totalOrders;
    private List<ProductCount> mostBoughtProducts;
    private List<CategoryPercentage> topCategories;

    @Data
    @AllArgsConstructor
    public static class ProductCount {
        private String name;
        private int count;
    }

    @Data
    @AllArgsConstructor
    public static class CategoryPercentage {
        private String name;
        private double percentage;
    }
}
