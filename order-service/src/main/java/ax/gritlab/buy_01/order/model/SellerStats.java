package ax.gritlab.buy_01.order.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerStats {
    private double totalRevenue;
    private int totalUnitsSold;
    private List<ProductRevenue> bestSellingProducts;
    private List<MonthlyRevenue> revenueByMonth;

    @Data
    @AllArgsConstructor
    public static class ProductRevenue {
        private String name;
        private double revenue;
        private int units;
    }

    @Data
    @AllArgsConstructor
    public static class MonthlyRevenue {
        private String month;
        private double amount;
    }
}
