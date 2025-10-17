package io.github.repoboard.dto.view;

import io.github.repoboard.model.User;
import io.github.repoboard.model.enums.UserRoleType;
import io.github.repoboard.model.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class UserView {

    private final String username;
    private final UserRoleType role;
    private final String provider;
    private final ProfileFullView profile;
    private final UserStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static UserView from(User user){
        if(user == null) return null;
        return new UserView(
                user.getUsername(),
                user.getRole(),
                user.getProvider() != null ? user.getProvider().name() : "LOCAL",
                user.getProfile() != null ? ProfileFullView.from(user.getProfile()) : null,
                user.getStatus() != null ? user.getStatus() : null,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}