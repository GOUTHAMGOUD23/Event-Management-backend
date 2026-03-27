package com.eventmanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events")
// Prevents Jackson from crashing on Hibernate proxy objects
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime startDateTime;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime endDateTime;

    @NotBlank
    @Column(nullable = false)
    private String location;

    @Positive
    @Column(nullable = false)
    private int maxCapacity;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private EventStatus status = EventStatus.UPCOMING;

    // Use EAGER for organizer so it's always available during serialization.
    // Alternatively keep LAZY and ensure open-in-view=true (set in application.properties).
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organizer_id", nullable = false)
    @JsonIgnoreProperties({"organizedEvents", "registrations", "password"})
    private User organizer;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Registration> registrations = new ArrayList<>();

    public enum EventStatus {
        UPCOMING, ONGOING, COMPLETED, CANCELLED
    }

    // ─── Constructors ─────────────────────────────────────────────────────────

    public Event() {}

    public Event(String title, String description,
                 LocalDateTime startDateTime, LocalDateTime endDateTime,
                 String location, int maxCapacity, User organizer) {
        this.title         = title;
        this.description   = description;
        this.startDateTime = startDateTime;
        this.endDateTime   = endDateTime;
        this.location      = location;
        this.maxCapacity   = maxCapacity;
        this.organizer     = organizer;
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }

    public String getTitle()                   { return title; }
    public void setTitle(String title)         { this.title = title; }

    public String getDescription()             { return description; }
    public void setDescription(String d)       { this.description = d; }

    public LocalDateTime getStartDateTime()               { return startDateTime; }
    public void setStartDateTime(LocalDateTime s)         { this.startDateTime = s; }

    public LocalDateTime getEndDateTime()                 { return endDateTime; }
    public void setEndDateTime(LocalDateTime e)           { this.endDateTime = e; }

    public String getLocation()                { return location; }
    public void setLocation(String location)   { this.location = location; }

    public int getMaxCapacity()                { return maxCapacity; }
    public void setMaxCapacity(int maxCapacity){ this.maxCapacity = maxCapacity; }

    public EventStatus getStatus()             { return status; }
    public void setStatus(EventStatus status)  { this.status = status; }

    public User getOrganizer()                 { return organizer; }
    public void setOrganizer(User organizer)   { this.organizer = organizer; }

    public List<Registration> getRegistrations()                   { return registrations; }
    public void setRegistrations(List<Registration> registrations) { this.registrations = registrations; }
}