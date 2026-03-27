package com.eventmanagement.controller;

import com.eventmanagement.model.Event;
import com.eventmanagement.service.EventService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "https://localhost:3000")
public class EventController {

    @Autowired
    private EventService eventService;

    // ═══════════════════════════════════════════════════════════════════════════
    // CREATE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * POST /api/events?organizerId={id}
     * Creates a new event owned by the given organizer.
     */
    @PostMapping
    public ResponseEntity<?> createEvent(
            @Valid @RequestBody Event event,
            @RequestParam Long organizerId) {

        Event created = eventService.createEvent(event, organizerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // READ
    // ═══════════════════════════════════════════════════════════════════════════

    /** GET /api/events — returns all events */
    @GetMapping
    public ResponseEntity<List<Event>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    /** GET /api/events/upcoming — returns only UPCOMING events, ordered by start date */
    @GetMapping("/upcoming")
    public ResponseEntity<List<Event>> getUpcomingEvents() {
        return ResponseEntity.ok(eventService.getUpcomingEvents());
    }

    /** GET /api/events/{id} — returns a single event by ID */
    @GetMapping("/{id}")
    public ResponseEntity<?> getEventById(@PathVariable Long id) {
        return eventService.getEventById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(error("Event not found with id: " + id)));
    }

    /** GET /api/events/organizer/{organizerId} — all events by a specific organizer */
    @GetMapping("/organizer/{organizerId}")
    public ResponseEntity<List<Event>> getEventsByOrganizer(@PathVariable Long organizerId) {
        return ResponseEntity.ok(eventService.getEventsByOrganizer(organizerId));
    }

    /**
     * GET /api/events/search?title=...&location=...
     * At least one param must be provided; falls back to all events if neither is given.
     */
    @GetMapping("/search")
    public ResponseEntity<List<Event>> searchEvents(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String location) {

        if (title != null && !title.isBlank()) {
            return ResponseEntity.ok(eventService.searchEventsByTitle(title));
        }
        if (location != null && !location.isBlank()) {
            return ResponseEntity.ok(eventService.searchEventsByLocation(location));
        }
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    /** GET /api/events/{id}/registrations/count — confirmed registration count */
    @GetMapping("/{id}/registrations/count")
    public ResponseEntity<?> getRegistrationCount(@PathVariable Long id) {
        long count = eventService.getConfirmedRegistrationCount(id);
        Map<String, Long> resp = new HashMap<>();
        resp.put("confirmedCount", count);
        return ResponseEntity.ok(resp);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * PUT /api/events/{id}?requestingUserId={id}
     * Full update — only the owner or ADMIN can update.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody Event updatedEvent,
            @RequestParam Long requestingUserId) {

        Event updated = eventService.updateEvent(id, updatedEvent, requestingUserId);
        return ResponseEntity.ok(updated);
    }

    /**
     * PATCH /api/events/{id}/cancel?requestingUserId={id}
     * Cancels the event and all its registrations.
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<?> cancelEvent(
            @PathVariable Long id,
            @RequestParam Long requestingUserId) {

        eventService.cancelEvent(id, requestingUserId);
        return ResponseEntity.ok(message("Event cancelled successfully."));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DELETE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * DELETE /api/events/{id}?requestingUserId={id}
     * Permanently deletes an event — only owner or ADMIN.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(
            @PathVariable Long id,
            @RequestParam Long requestingUserId) {

        eventService.deleteEvent(id, requestingUserId);
        return ResponseEntity.ok(message("Event deleted successfully."));
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