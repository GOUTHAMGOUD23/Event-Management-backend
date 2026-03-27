package com.eventmanagement.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventmanagement.model.Event;
import com.eventmanagement.model.Registration;
import com.eventmanagement.model.User;
import com.eventmanagement.repository.EventRepository;
import com.eventmanagement.repository.RegistrationRepository;
import com.eventmanagement.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    // ═══════════════════════════════════════════════════════════════════════════
    // EVENT CRUD
    // ═══════════════════════════════════════════════════════════════════════════

    public Event createEvent(Event event, Long organizerId) {
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new RuntimeException("Organizer not found with id: " + organizerId));

        if (organizer.getRole() != User.Role.ORGANIZER && organizer.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Only users with ORGANIZER or ADMIN role can create events.");
        }

        // Validate dates
        if (event.getStartDateTime() != null && event.getEndDateTime() != null
                && !event.getEndDateTime().isAfter(event.getStartDateTime())) {
            throw new RuntimeException("End date/time must be after start date/time.");
        }

        event.setOrganizer(organizer);
        event.setStatus(Event.EventStatus.UPCOMING);
        return eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Event> getEventById(Long id) {
        return eventRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Event> getUpcomingEvents() {
        return eventRepository.findUpcomingEvents(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<Event> getEventsByOrganizer(Long organizerId) {
        return eventRepository.findByOrganizerId(organizerId);
    }

    @Transactional(readOnly = true)
    public List<Event> searchEventsByTitle(String keyword) {
        return eventRepository.findByTitleContainingIgnoreCase(keyword);
    }

    @Transactional(readOnly = true)
    public List<Event> searchEventsByLocation(String location) {
        return eventRepository.findByLocationContainingIgnoreCase(location);
    }

    public Event updateEvent(Long id, Event updatedEvent, Long requestingUserId) {
        Event existing = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));

        User requester = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + requestingUserId));

        // Only the organizer or an admin can update
        boolean isOwner = existing.getOrganizer().getId().equals(requestingUserId);
        boolean isAdmin = requester.getRole() == User.Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new RuntimeException("You are not authorized to update this event.");
        }

        if (existing.getStatus() == Event.EventStatus.CANCELLED) {
            throw new RuntimeException("Cannot update a cancelled event.");
        }

        if (updatedEvent.getEndDateTime() != null && updatedEvent.getStartDateTime() != null
                && !updatedEvent.getEndDateTime().isAfter(updatedEvent.getStartDateTime())) {
            throw new RuntimeException("End date/time must be after start date/time.");
        }

        existing.setTitle(updatedEvent.getTitle());
        existing.setDescription(updatedEvent.getDescription());
        existing.setStartDateTime(updatedEvent.getStartDateTime());
        existing.setEndDateTime(updatedEvent.getEndDateTime());
        existing.setLocation(updatedEvent.getLocation());
        existing.setMaxCapacity(updatedEvent.getMaxCapacity());

        // Only allow status changes within valid transitions
        if (updatedEvent.getStatus() != null) {
            existing.setStatus(updatedEvent.getStatus());
        }

        return eventRepository.save(existing);
    }

    public void cancelEvent(Long id, Long requestingUserId) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));

        User requester = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found."));

        boolean isOwner = event.getOrganizer().getId().equals(requestingUserId);
        boolean isAdmin = requester.getRole() == User.Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new RuntimeException("You are not authorized to cancel this event.");
        }

        if (event.getStatus() == Event.EventStatus.CANCELLED) {
            throw new RuntimeException("Event is already cancelled.");
        }

        event.setStatus(Event.EventStatus.CANCELLED);
        eventRepository.save(event);

        // Bulk-cancel all active registrations
        List<Registration> active = registrationRepository.findByEventId(id);
        active.forEach(r -> {
            if (r.getStatus() != Registration.RegistrationStatus.CANCELLED) {
                r.setStatus(Registration.RegistrationStatus.CANCELLED);
            }
        });
        registrationRepository.saveAll(active);
    }

    public void deleteEvent(Long id, Long requestingUserId) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));

        User requester = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found."));

        boolean isOwner = event.getOrganizer().getId().equals(requestingUserId);
        boolean isAdmin = requester.getRole() == User.Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new RuntimeException("You are not authorized to delete this event.");
        }

        eventRepository.deleteById(id);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REGISTRATION LOGIC
    // ═══════════════════════════════════════════════════════════════════════════

    public Registration registerForEvent(Long userId, Long eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));

        if (registrationRepository.existsByUserAndEvent(user, event)) {
            throw new RuntimeException("You are already registered for this event.");
        }

        if (event.getStatus() != Event.EventStatus.UPCOMING) {
            throw new RuntimeException("Registrations are only open for upcoming events.");
        }

        long confirmed = registrationRepository.countConfirmedByEventId(eventId);
        Registration registration = new Registration(user, event);

        if (confirmed >= event.getMaxCapacity()) {
            registration.setStatus(Registration.RegistrationStatus.WAITLISTED);
        } else {
            registration.setStatus(Registration.RegistrationStatus.CONFIRMED);
        }

        return registrationRepository.save(registration);
    }

    public void cancelRegistration(Long registrationId, Long userId) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Registration not found with id: " + registrationId));

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found."));

        // Owner or Admin can cancel
        boolean isOwner = registration.getUser().getId().equals(userId);
        boolean isAdmin = requester.getRole() == User.Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new RuntimeException("You are not authorized to cancel this registration.");
        }

        if (registration.getStatus() == Registration.RegistrationStatus.CANCELLED) {
            throw new RuntimeException("Registration is already cancelled.");
        }

        boolean wasConfirmed = registration.getStatus() == Registration.RegistrationStatus.CONFIRMED;
        registration.setStatus(Registration.RegistrationStatus.CANCELLED);
        registrationRepository.save(registration);

        // If a confirmed seat opened up, promote the first waitlisted person
        if (wasConfirmed) {
            promoteWaitlisted(registration.getEvent().getId());
        }
    }

    private void promoteWaitlisted(Long eventId) {
        List<Registration> waitlisted = registrationRepository
                .findByEventIdAndStatus(eventId, Registration.RegistrationStatus.WAITLISTED);

        if (!waitlisted.isEmpty()) {
            Registration next = waitlisted.get(0);
            next.setStatus(Registration.RegistrationStatus.CONFIRMED);
            registrationRepository.save(next);
        }
    }

    @Transactional(readOnly = true)
    public List<Registration> getRegistrationsByEvent(Long eventId) {
        return registrationRepository.findByEventIdWithDetails(eventId);
    }

    @Transactional(readOnly = true)
    public List<Registration> getRegistrationsByUser(Long userId) {
        return registrationRepository.findByUserIdWithDetails(userId);
    }

    @Transactional(readOnly = true)
    public long getConfirmedRegistrationCount(Long eventId) {
        return registrationRepository.countConfirmedByEventId(eventId);
    }
}