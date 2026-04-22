package com.ticks.event_service.dto.response;

import com.ticks.event_service.entity.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private UUID id;
    private String title;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    private EventStatus status;
    private UUID organizerId;
    private VenueResponseDTO venue;
    private CategoryResponseDTO category;
    private List<ScheduleResponseDTO> schedules;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}