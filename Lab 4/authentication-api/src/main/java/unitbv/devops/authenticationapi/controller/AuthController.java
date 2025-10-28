package unitbv.devops.authenticationapi.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import unitbv.devops.authenticationapi.dto.*;
import unitbv.devops.authenticationapi.dto.auth.LoginRequest;
import unitbv.devops.authenticationapi.dto.auth.LoginResponse;
import unitbv.devops.authenticationapi.dto.auth.RegisterRequest;
import unitbv.devops.authenticationapi.user.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService service;

    public AuthController(UserService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest request) {
        return service.register(request)
                .<ResponseEntity<?>>map(user -> ResponseEntity.status(HttpStatus.CREATED).body(user))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new SimpleError("Username or email already in use")));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return service.login(request)
                .map(user -> ResponseEntity.ok(new LoginResponse(true, user)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new LoginResponse(false, null)));
    }

    public record SimpleError(String error) {}
}
