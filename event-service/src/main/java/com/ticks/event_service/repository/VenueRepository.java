package com.ticks.event_service.repository;

import com.ticks.event_service.entity.Venue;
import com.ticks.event_service.entity.VenueStatus;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface VenueRepository extends JpaRepository<Venue, UUID> {

    Page<Venue> findByStatus(VenueStatus status, Pageable pageable);

    Page<Venue> findByCityIgnoreCaseAndStatus(String city, VenueStatus status, Pageable pageable);

    boolean existsByNameAndCity(String name, String city);

    @Modifying
    @Query("UPDATE Venue v SET v.status = :status WHERE v.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") VenueStatus status);
}
