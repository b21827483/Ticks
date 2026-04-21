package com.ticks.event_service.repository;

import com.ticks.event_service.entity.EventSchedule;
import com.ticks.event_service.entity.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface EventScheduleRepository extends JpaRepository<EventSchedule, UUID> {

    List<EventSchedule> findByEventIdOrderByStartTime(UUID eventId);

    List<EventSchedule> findByEventIdAndStatus(UUID eventId, ScheduleStatus status);

    @Query("""
           SELECT s FROM EventSchedule s
           WHERE s.status = 'ON_SALE'
           AND s.endTime < :cutoff
           """)
    List<EventSchedule> findEndedSchedules(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query("UPDATE EventSchedule s SET s.status = :status WHERE s.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") ScheduleStatus status);

    @Modifying
    @Query("UPDATE EventSchedule s SET s.status = :status WHERE s.event.id = :eventId")
    void updateStatusByEventId(@Param("eventId") UUID eventId,
                               @Param("status") ScheduleStatus status);
}
