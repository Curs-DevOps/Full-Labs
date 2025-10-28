package unitbv.devops.authenticationapi.user.service;

import unitbv.devops.authenticationapi.dto.auth.LoginRequest;
import unitbv.devops.authenticationapi.dto.auth.RegisterRequest;
import unitbv.devops.authenticationapi.dto.auth.UserResponse;
import java.util.Optional;

public interface UserService {
    public Optional<UserResponse> register(RegisterRequest req);
    public Optional<UserResponse> login(LoginRequest req);
}
