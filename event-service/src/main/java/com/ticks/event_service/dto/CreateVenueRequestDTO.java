package com.ticks.event_service.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateVenueRequestDTO {

    @NotBlank(message = "Name is required")
    @Size(max = 150)
    private String name;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @NotBlank(message = "Country is required")
    @Size(max = 100)
    private String country;

    @Size(max = 20)
    private String postalCode;

    @Min(value = 1, message = "Capacity must be at least 1")
    private int capacity;

    private String description;
    private String imageUrl;
}