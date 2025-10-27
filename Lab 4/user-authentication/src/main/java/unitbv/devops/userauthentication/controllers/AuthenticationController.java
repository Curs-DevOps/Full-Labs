package unitbv.devops.userauthentication.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import unitbv.devops.userauthentication.dtos.LoginRequest;
import unitbv.devops.userauthentication.dtos.RegisterRequest;
import unitbv.devops.userauthentication.dtos.UserView;
import unitbv.devops.userauthentication.models.User;
import unitbv.devops.userauthentication.services.AuthenticationService;

import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationService authService;

    public AuthenticationController(AuthenticationService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserView> register(@Valid @RequestBody RegisterRequest req) {
        User created = authService.register(req.email(), req.fullName(), req.password());
        return ResponseEntity.status(201).body(UserView.from(created));
    }

    @PostMapping("/login")
    public ResponseEntity<UserView> login(@Valid @RequestBody LoginRequest req) {
        User loggedIn = authService.login(req.email(), req.password());
        return ResponseEntity.ok(UserView.from(loggedIn));
    }
}
