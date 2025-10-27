package unitbv.devops.userauthentication.services;


import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import unitbv.devops.userauthentication.interfaces.UserRepository;
import unitbv.devops.userauthentication.models.User;

import java.util.Set;

@Service
public class AuthenticationService {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AuthenticationService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public User register(String email, String fullName, String rawPassword) {
        users.findByEmail(email).ifPresent(u -> {
            throw new IllegalArgumentException("Email already registered");
        });

        var hashed = encoder.encode(rawPassword);
        var user = User.createNew(email, fullName, hashed, Set.of("ROLE_USER"));
        return users.save(user);
    }

    public User login(String email, String rawPassword) {
        var user = users.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!encoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return user;
    }
}
