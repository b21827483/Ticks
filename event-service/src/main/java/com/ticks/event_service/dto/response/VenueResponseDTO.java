package com.ticks.event_service.dto.response;

import com.ticks.event_service.entity.VenueStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private UUID id;
    private String name;
    private String address;
    private String city;
    private String country;
    private String postalCode;
    private int capacity;
    private String description;
    private String imageUrl;
    private VenueStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}