package ax.gritlab.buy_01.order.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Shipping address embedded document.
 * Snapshotted at checkout time to preserve delivery information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingAddress {

    @NotNull
    private String fullName;

    @NotNull
    private String addressLine1;

    private String addressLine2;

    @NotNull
    private String city;

    @NotNull
    private String postalCode;

    @NotNull
    private String country;

    private String phoneNumber;

    /**
     * Returns formatted address for display.
     */
    public String getFormattedAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(fullName).append("\n");
        sb.append(addressLine1);
        if (addressLine2 != null && !addressLine2.isEmpty()) {
            sb.append(", ").append(addressLine2);
        }
        sb.append("\n");
        sb.append(city).append(", ").append(postalCode).append("\n");
        sb.append(country);
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            sb.append("\nPhone: ").append(phoneNumber);
        }
        return sb.toString();
    }
}
