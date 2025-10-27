package unitbv.devops.userauthentication.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email String email,
        @NotBlank @Size(min = 3, max = 80) String fullName,
        @NotBlank @Size(min = 6, max = 100) String password
) {}
