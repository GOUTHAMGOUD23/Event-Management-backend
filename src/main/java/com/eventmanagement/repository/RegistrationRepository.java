package com.eventmanagement.repository;

import com.eventmanagement.model.Registration;
import com.eventmanagement.model.Event;
import com.eventmanagement.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    List<Registration> findByUserId(Long userId);

    List<Registration> findByEventId(Long eventId);

    Optional<Registration> findByUserAndEvent(User user, Event event);

    boolean existsByUserAndEvent(User user, Event event);

    List<Registration> findByEventIdAndStatus(Long eventId, Registration.RegistrationStatus status);

    @Query("SELECT COUNT(r) FROM Registration r WHERE r.event.id = :eventId AND r.status = 'CONFIRMED'")
    long countConfirmedByEventId(@Param("eventId") Long eventId);

    // JOIN FETCH ensures user and event are loaded in one query — prevents LazyInitializationException
    @Query("SELECT r FROM Registration r JOIN FETCH r.event JOIN FETCH r.user WHERE r.user.id = :userId ORDER BY r.registeredAt DESC")
    List<Registration> findByUserIdWithDetails(@Param("userId") Long userId);

    // JOIN FETCH for event-side listing
    @Query("SELECT r FROM Registration r JOIN FETCH r.user WHERE r.event.id = :eventId ORDER BY r.registeredAt ASC")
    List<Registration> findByEventIdWithDetails(@Param("eventId") Long eventId);
}