package ax.gritlab.buy_01.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private String id;
    private String name;
    private String description;
    private Double price;
    private Integer stock;
    private String sellerId;
    private List<String> mediaIds;
    private List<String> imageUrls;
    private String createdAt;
    private String updatedAt;
}
