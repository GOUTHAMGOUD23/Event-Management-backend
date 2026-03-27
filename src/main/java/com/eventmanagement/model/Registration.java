package com.eventmanagement.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@Entity
@Table(name = "registrations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "event_id"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private LocalDateTime registeredAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private RegistrationStatus status = RegistrationStatus.CONFIRMED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum RegistrationStatus {
        CONFIRMED, WAITLISTED, CANCELLED
    }

    // ─── Constructors ─────────────────────────────────────────────────────────

    public Registration() {}

    public Registration(User user, Event event) {
        this.user         = user;
        this.event        = event;
        this.registeredAt = LocalDateTime.now();
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public Long getId()                              { return id; }
    public void setId(Long id)                       { this.id = id; }

    public User getUser()                            { return user; }
    public void setUser(User user)                   { this.user = user; }

    public Event getEvent()                          { return event; }
    public void setEvent(Event event)                { this.event = event; }

    public LocalDateTime getRegisteredAt()           { return registeredAt; }
    public void setRegisteredAt(LocalDateTime r)     { this.registeredAt = r; }

    public RegistrationStatus getStatus()            { return status; }
    public void setStatus(RegistrationStatus status) { this.status = status; }

    public String getNotes()                         { return notes; }
    public void setNotes(String notes)               { this.notes = notes; }
}