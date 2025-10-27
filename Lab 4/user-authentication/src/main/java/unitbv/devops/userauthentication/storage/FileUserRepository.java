package unitbv.devops.userauthentication.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import unitbv.devops.userauthentication.interfaces.UserRepository;
import unitbv.devops.userauthentication.models.User;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Repository
public class FileUserRepository implements UserRepository {

    private final ObjectMapper mapper;
    private final Path storePath;

    private final Map<String, User> users = new HashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public FileUserRepository(
            ObjectMapper mapper,
            @Value("${app.storage.users-file:data/users.json}") String usersFile
    ) {
        this.mapper = mapper;
        this.storePath = Paths.get(usersFile);
        initStore();
    }

    private void initStore() {
        try {
            if (storePath.getParent() != null) {
                Files.createDirectories(storePath.getParent());
            }
            if (Files.exists(storePath) && Files.size(storePath) > 0) {
                var list = mapper.readValue(Files.readAllBytes(storePath),
                        new TypeReference<List<User>>() {});
                for (User u : list) {
                    users.put(normalize(u.getEmail()), u);
                }
            } else {
                persist();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize user store", e);
        }
    }

    private static String normalize(String email) {
        return email == null ? null : email.toLowerCase(Locale.ROOT);
    }

    private void persist() {
        lock.writeLock().lock();
        try {
            var list = new ArrayList<>(users.values());
            mapper.writerWithDefaultPrettyPrinter().writeValue(storePath.toFile(), list);
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist users file", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(users.get(normalize(email)));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public User save(User user) {
        lock.writeLock().lock();
        try {
            users.put(normalize(user.getEmail()), user);
            persist();
            return user;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<User> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(users.values());
        } finally {
            lock.readLock().unlock();
        }
    }
}

