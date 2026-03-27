package com.eventmanagement.controller;

import com.eventmanagement.model.Registration;
import com.eventmanagement.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/registrations")
@CrossOrigin(origins = "https://localhost:3000")
public class RegistrationController {

    @Autowired
    private EventService eventService;

    // ═══════════════════════════════════════════════════════════════════════════
    // REGISTER USER FOR EVENT
    // POST /api/registrations?userId={id}&eventId={id}
    // ═══════════════════════════════════════════════════════════════════════════

    @PostMapping
    public ResponseEntity<?> registerForEvent(
            @RequestParam Long userId,
            @RequestParam Long eventId) {

        Registration reg = eventService.registerForEvent(userId, eventId);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(reg));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CANCEL REGISTRATION
    // PATCH /api/registrations/{registrationId}/cancel?userId={id}
    // ═══════════════════════════════════════════════════════════════════════════

    @PatchMapping("/{registrationId}/cancel")
    public ResponseEntity<?> cancelRegistration(
            @PathVariable Long registrationId,
            @RequestParam Long userId) {

        eventService.cancelRegistration(registrationId, userId);
        return ResponseEntity.ok(message("Registration cancelled successfully."));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GET REGISTRATIONS BY EVENT
    // GET /api/registrations/event/{eventId}
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<Map<String, Object>>> getRegistrationsByEvent(
            @PathVariable Long eventId) {

        List<Map<String, Object>> dtos = eventService.getRegistrationsByEvent(eventId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GET REGISTRATIONS BY USER
    // GET /api/registrations/user/{userId}
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getRegistrationsByUser(
            @PathVariable Long userId) {

        List<Map<String, Object>> dtos = eventService.getRegistrationsByUser(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DTO MAPPER
    // Converts Registration entity → flat Map to avoid:
    //   1. Jackson infinite recursion (Registration ↔ Event ↔ Registration…)
    //   2. LazyInitializationException from Hibernate proxies
    //   3. Accidental password exposure
    // ═══════════════════════════════════════════════════════════════════════════

    private Map<String, Object> toDto(Registration r) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id",           r.getId());
        dto.put("status",       r.getStatus() != null ? r.getStatus().name() : null);
        dto.put("registeredAt", r.getRegisteredAt());
        dto.put("notes",        r.getNotes());

        // Flatten event — only the fields the frontend needs
        if (r.getEvent() != null) {
            Map<String, Object> ev = new HashMap<>();
            ev.put("id",            r.getEvent().getId());
            ev.put("title",         r.getEvent().getTitle());
            ev.put("description",   r.getEvent().getDescription());
            ev.put("location",      r.getEvent().getLocation());
            ev.put("startDateTime", r.getEvent().getStartDateTime());
            ev.put("endDateTime",   r.getEvent().getEndDateTime());
            ev.put("status",        r.getEvent().getStatus() != null ? r.getEvent().getStatus().name() : null);
            ev.put("maxCapacity",   r.getEvent().getMaxCapacity());

            // Organizer summary (no password)
            if (r.getEvent().getOrganizer() != null) {
                Map<String, Object> org = new HashMap<>();
                org.put("id",       r.getEvent().getOrganizer().getId());
                org.put("username", r.getEvent().getOrganizer().getUsername());
                ev.put("organizer", org);
            }

            dto.put("event", ev);
        }

        // Flatten user — no password
        if (r.getUser() != null) {
            Map<String, Object> u = new HashMap<>();
            u.put("id",       r.getUser().getId());
            u.put("username", r.getUser().getUsername());
            u.put("email",    r.getUser().getEmail());
            u.put("role",     r.getUser().getRole() != null ? r.getUser().getRole().name() : null);
            dto.put("user", u);
        }

        return dto;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private Map<String, String> error(String msg) {
        Map<String, String> m = new HashMap<>();
        m.put("error", msg);
        return m;
    }

    private Map<String, String> message(String msg) {
        Map<String, String> m = new HashMap<>();
        m.put("message", msg);
        return m;
    }
}