package unitbv.devops.authenticationapi.user.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import unitbv.devops.authenticationapi.dto.auth.LoginRequest;
import unitbv.devops.authenticationapi.dto.auth.RegisterRequest;
import unitbv.devops.authenticationapi.dto.auth.UserResponse;
import unitbv.devops.authenticationapi.user.entity.Role;
import unitbv.devops.authenticationapi.user.entity.User;
import unitbv.devops.authenticationapi.user.mapper.UserMapper;
import unitbv.devops.authenticationapi.user.repository.UserRepository;
import unitbv.devops.authenticationapi.user.service.UserService;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserServiceImpl(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public Optional<UserResponse> register(RegisterRequest req) {
        if (users.existsByUsername(req.username()) || users.existsByEmail(req.email())) {
            return Optional.empty();
        }

        User u = User.builder()
                .username(req.username())
                .email(req.email())
                .passwordHash(encoder.encode(req.password()))
                .roles(new HashSet<>(Set.of(Role.USER)))
                .createdAt(Instant.now())
                .enabled(true)
                .build();

        u = users.save(u);
        return Optional.of(UserMapper.toResponse(u));
    }

    @Override
    public Optional<UserResponse> login(LoginRequest req) {
        Optional<User> found = users.findByUsername(req.usernameOrEmail());
        if (found.isEmpty()) {
            found = users.findByEmail(req.usernameOrEmail());
        }

        if (found.isEmpty()) {
            return Optional.empty();
        }

        User u = found.get();
        if (!encoder.matches(req.password(), u.getPasswordHash())) {
            return Optional.empty();
        }

        return Optional.of(UserMapper.toResponse(u));
    }
}
