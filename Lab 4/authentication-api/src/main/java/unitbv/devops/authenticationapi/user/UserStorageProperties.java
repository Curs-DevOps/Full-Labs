package unitbv.devops.authenticationapi.user;

import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Configuration
@ConfigurationProperties(prefix = "user.storage")
public class UserStorageProperties {

    private String filePath = "data/users.json";

    public String filePath() { return filePath; }
}
