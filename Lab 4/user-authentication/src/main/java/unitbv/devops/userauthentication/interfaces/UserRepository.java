package unitbv.devops.userauthentication.interfaces;

import unitbv.devops.userauthentication.models.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByEmail(String email);

    User save(User user);

    List<User> findAll();
}
