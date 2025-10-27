package unitbv.devops.userauthentication.models;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class User {
    private String id;
    private String email;
    private String fullName;
    private String passwordHash;
    private Set<String> roles;

    public User() {

    }

    public User(String id, String email, String fullName, String passwordHash, Set<String> roles) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.roles = roles;
    }

    public static User createNew(String email, String fullName, String passwordHash, Set<String> roles) {
        return new User(UUID.randomUUID().toString(), email, fullName, passwordHash, roles);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return Objects.equals(email == null ? null : email.toLowerCase(),
                user.email == null ? null : user.email.toLowerCase());
    }

    @Override
    public int hashCode() {
        return Objects.hash(email == null ? null : email.toLowerCase());
    }

    @Override
    public String toString() {
        return "User{id='%s', email='%s', fullName='%s', roles=%s}"
                .formatted(id, email, fullName, roles);
    }
}
