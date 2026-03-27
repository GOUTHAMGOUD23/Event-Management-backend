package com.eventmanagement.controller;

import com.eventmanagement.model.User;
import com.eventmanagement.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "https://localhost:3000")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ═══════════════════════════════════════════════════════════════════════════
    // REGISTER
    // ═══════════════════════════════════════════════════════════════════════════

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest req) {

        if (userRepository.existsByUsername(req.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(error("Username '" + req.getUsername() + "' is already taken."));
        }

        if (userRepository.existsByEmail(req.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(error("Email '" + req.getEmail() + "' is already in use."));
        }

        User user = new User(
                req.getUsername(),
                req.getEmail(),
                passwordEncoder.encode(req.getPassword())
        );

        if (req.getRole() != null) {
            user.setRole(req.getRole());
        }

        User saved = userRepository.save(user);

        Map<String, Object> resp = new HashMap<>();
        resp.put("message",  "Registration successful.");
        resp.put("userId",   saved.getId());
        resp.put("username", saved.getUsername());
        resp.put("email",    saved.getEmail());
        resp.put("role",     saved.getRole());

        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LOGIN
    // ═══════════════════════════════════════════════════════════════════════════

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest req) {

        if (req.getUsername() == null || req.getUsername().isBlank()
                || req.getPassword() == null || req.getPassword().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(error("Username and password are required."));
        }

        User user = userRepository.findByUsername(req.getUsername().trim())
                .orElse(null);

        // Use constant-time comparison to prevent user-enumeration attacks
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(error("Invalid username or password."));
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("message",  "Login successful.");
        resp.put("userId",   user.getId());
        resp.put("username", user.getUsername());
        resp.put("email",    user.getEmail());
        resp.put("role",     user.getRole());

        return ResponseEntity.ok(resp);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GET USER BY ID (useful for session restore)
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/user/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .<ResponseEntity<?>>map(u -> {
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("userId",   u.getId());
                    resp.put("username", u.getUsername());
                    resp.put("email",    u.getEmail());
                    resp.put("role",     u.getRole());
                    return ResponseEntity.ok(resp);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(error("User not found.")));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private Map<String, String> error(String message) {
        Map<String, String> m = new HashMap<>();
        m.put("error", message);
        return m;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REQUEST BODIES
    // ═══════════════════════════════════════════════════════════════════════════

    public static class RegisterRequest {

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be 3–50 characters")
        private String username;

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 100)
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 40, message = "Password must be 6–40 characters")
        private String password;

        private User.Role role;   // optional; defaults to USER if omitted

        public String getUsername()          { return username; }
        public void setUsername(String u)    { this.username = u; }

        public String getEmail()             { return email; }
        public void setEmail(String e)       { this.email = e; }

        public String getPassword()          { return password; }
        public void setPassword(String p)    { this.password = p; }

        public User.Role getRole()           { return role; }
        public void setRole(User.Role role)  { this.role = role; }
    }

    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername()          { return username; }
        public void setUsername(String u)    { this.username = u; }

        public String getPassword()          { return password; }
        public void setPassword(String p)    { this.password = p; }
    }
}