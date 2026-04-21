package com.ticks.event_service.repository;

import com.ticks.event_service.entity.Event;
import com.ticks.event_service.entity.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    Page<Event> findByCategoryIdAndStatus(UUID categoryId, EventStatus status, Pageable pageable);

    Page<Event> findByVenueIdAndStatus(UUID venueId, EventStatus status, Pageable pageable);

    Page<Event> findByOrganizerIdAndStatus(UUID organizerId, EventStatus status, Pageable pageable);

    @Query("""
           SELECT e FROM Event e
           WHERE e.status = :status
           AND LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
           """)
    Page<Event> searchByTitle(@Param("keyword") String keyword,
                              @Param("status") EventStatus status,
                              Pageable pageable);

    @Modifying
    @Query("UPDATE Event e SET e.status = :status WHERE e.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") EventStatus status);
}
