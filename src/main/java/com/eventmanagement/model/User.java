package com.eventmanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
})
// Prevents Jackson from crashing on Hibernate proxy objects
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, unique = true)
    private String username;

    @NotBlank
    @Size(max = 100)
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    @Size(max = 120)
    @JsonIgnore   // Never serialize password in API responses
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Role role = Role.USER;

    // Exclude collections from serialization — use DTOs instead
    @OneToMany(mappedBy = "organizer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Event> organizedEvents = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Registration> registrations = new ArrayList<>();

    public enum Role {
        USER, ADMIN, ORGANIZER
    }

    // ─── Constructors ─────────────────────────────────────────────────────────

    public User() {}

    public User(String username, String email, String password) {
        this.username = username;
        this.email    = email;
        this.password = password;
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }

    public String getUsername()                { return username; }
    public void setUsername(String username)   { this.username = username; }

    public String getEmail()                   { return email; }
    public void setEmail(String email)         { this.email = email; }

    public String getPassword()                { return password; }
    public void setPassword(String password)   { this.password = password; }

    public Role getRole()                      { return role; }
    public void setRole(Role role)             { this.role = role; }

    public List<Event> getOrganizedEvents()                        { return organizedEvents; }
    public void setOrganizedEvents(List<Event> organizedEvents)    { this.organizedEvents = organizedEvents; }

    public List<Registration> getRegistrations()                   { return registrations; }
    public void setRegistrations(List<Registration> registrations) { this.registrations = registrations; }
}