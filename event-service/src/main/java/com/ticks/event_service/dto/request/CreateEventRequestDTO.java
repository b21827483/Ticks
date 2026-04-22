package com.ticks.event_service.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateEventRequestDTO {

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;

    private String description;
    private String imageUrl;

    @DecimalMin(value = "0.0", inclusive = false, message = "Max price must be positive")
    private BigDecimal price;

    @NotNull(message = "Venue ID is required")
    private UUID venueId;

    @NotNull(message = "Category ID is required")
    private UUID categoryId;
}