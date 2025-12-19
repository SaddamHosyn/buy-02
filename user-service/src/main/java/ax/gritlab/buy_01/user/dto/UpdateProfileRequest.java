package ax.gritlab.buy_01.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileRequest {
    private String name;
    private String avatar;
    private String password; // Current password for verification
    private String newPassword; // New password to set
}
