package unitbv.devops.userauthentication.dtos;

import unitbv.devops.userauthentication.models.User;

import java.util.Set;

public record UserView(
        String id,
        String email,
        String fullName,
        Set<String> roles
) {
    public static UserView from(User u) {
        return new UserView(u.getId(), u.getEmail(), u.getFullName(), u.getRoles());
    }
}
