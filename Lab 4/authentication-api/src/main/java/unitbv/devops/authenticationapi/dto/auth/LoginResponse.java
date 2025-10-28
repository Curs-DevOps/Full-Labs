package unitbv.devops.authenticationapi.dto.auth;

import unitbv.devops.authenticationapi.dto.auth.UserResponse;

public record LoginResponse(
        boolean authenticated,
        UserResponse user
) {}
