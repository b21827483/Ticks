package com.ticks.event_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateCategoryRequestDTO {

    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;

    @Size(max = 100)
    private String slug;

    private String description;
    private String imageUrl;

    private UUID parentId;
}