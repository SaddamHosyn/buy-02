package ax.gritlab.buy_01.order.dto;

import lombok.Data;

@Data
public class ProductDTO {
    private String id;
    private String name;
    private Double price;
    private Integer stock;
    private String sellerId;
}
