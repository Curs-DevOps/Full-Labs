package unitbv.devops.authenticationapi.user;

import lombok.*;

import java.time.Instant;
import java.util.Set;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    private String id;
    private String username;
    private String email;
    private String passwordHash;
    private Set<Role> roles;
    private Instant createdAt;
    private boolean enabled;
}
