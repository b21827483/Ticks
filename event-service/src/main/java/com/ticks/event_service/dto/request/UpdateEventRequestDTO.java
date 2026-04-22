package com.ticks.event_service.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class UpdateEventRequestDTO {

    @Size(max = 200)
    private String title;

    private String description;
    private String imageUrl;

    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;

    private UUID venueId;
    private UUID categoryId;
}