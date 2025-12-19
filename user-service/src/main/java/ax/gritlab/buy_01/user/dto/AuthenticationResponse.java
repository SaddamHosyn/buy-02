package ax.gritlab.buy_01.user.dto;

import ax.gritlab.buy_01.user.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {
    private String token;
    private String id;
    private String email;
    private String name;
    private Role role;
    private String avatarUrl;
}
