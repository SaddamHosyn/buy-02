package ax.gritlab.buy_01.user.service;

import ax.gritlab.buy_01.user.dto.UpdateProfileRequest;
import ax.gritlab.buy_01.user.dto.UserProfileResponse;
import ax.gritlab.buy_01.user.model.User;
import ax.gritlab.buy_01.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.kafka.core.KafkaTemplate;

@Service
@RequiredArgsConstructor
public class UserService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileResponse getProfile(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .avatar(user.getAvatar())
                .build();
    }

    // NEW: Get user by ID (for viewing seller profiles)
    public UserProfileResponse getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return getProfile(user);
    }

    public UserProfileResponse updateProfile(User user, UpdateProfileRequest request) {
        // 1. Handle Password Change Logic
        if (request.getNewPassword() != null && !request.getNewPassword().isEmpty()) {
            // Must provide current password to change it
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                throw new IllegalArgumentException("Current password is required to set a new password");
            }

            // Verify current password matches DB
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new RuntimeException("Incorrect current password");
            }

            // Update to new password
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        // 2. Handle Name Update
        if (request.getName() != null && !request.getName().isEmpty()) {
            user.setName(request.getName());
        }

        // 3. Handle Avatar Update
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        User updatedUser = userRepository.save(user);
        return getProfile(updatedUser);
    }

    public void deleteUser(User user) {
        userRepository.delete(user);
        // Publish Kafka event for user deletion
        kafkaTemplate.send("user.deleted", user.getId());
    }
}
