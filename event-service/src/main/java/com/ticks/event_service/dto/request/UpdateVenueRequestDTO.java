package com.ticks.event_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateVenueRequestDTO {

    @Size(max = 150)
    private String name;

    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String country;

    @Size(max = 20)
    private String postalCode;

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    private String description;
    private String imageUrl;
}