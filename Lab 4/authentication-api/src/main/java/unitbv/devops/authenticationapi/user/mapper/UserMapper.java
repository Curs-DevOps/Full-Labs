package unitbv.devops.authenticationapi.user.mapper;

import unitbv.devops.authenticationapi.dto.auth.UserResponse;
import unitbv.devops.authenticationapi.user.entity.Role;
import unitbv.devops.authenticationapi.user.entity.User;

import java.util.Set;
import java.util.stream.Collectors;

public final class UserMapper {
    private UserMapper() {}

    public static UserResponse toResponse(User u) {
        Set<String> roles = u.getRoles() == null ? Set.of()
                : u.getRoles().stream().map(Role::name).collect(Collectors.toSet());
        return new UserResponse(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                roles,
                u.getCreatedAt(),
                u.isEnabled()
        );
    }
}
