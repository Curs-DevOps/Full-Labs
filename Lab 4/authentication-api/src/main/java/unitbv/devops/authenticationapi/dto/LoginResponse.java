package unitbv.devops.authenticationapi.dto;

import unitbv.devops.authenticationapi.dto.UserResponse;

public record LoginResponse(
        boolean authenticated,
        UserResponse user
) {}
