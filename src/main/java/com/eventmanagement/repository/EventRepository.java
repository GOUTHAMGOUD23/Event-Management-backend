package com.eventmanagement.repository;

import com.eventmanagement.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByStatus(Event.EventStatus status);

    List<Event> findByTitleContainingIgnoreCase(String keyword);

    List<Event> findByLocationContainingIgnoreCase(String location);

    List<Event> findByStartDateTimeAfter(LocalDateTime dateTime);

    List<Event> findByStartDateTimeBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT e FROM Event e WHERE e.status = 'UPCOMING' AND e.startDateTime > :now ORDER BY e.startDateTime ASC")
    List<Event> findUpcomingEvents(@Param("now") LocalDateTime now);

    @Query("SELECT e FROM Event e WHERE e.organizer.id = :organizerId ORDER BY e.startDateTime DESC")
    List<Event> findByOrganizerId(@Param("organizerId") Long organizerId);

    @Query("SELECT COUNT(r) FROM Registration r WHERE r.event.id = :eventId AND r.status = 'CONFIRMED'")
    long countConfirmedRegistrations(@Param("eventId") Long eventId);
}