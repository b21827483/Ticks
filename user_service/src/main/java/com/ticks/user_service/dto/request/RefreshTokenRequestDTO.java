package com.ticks.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequestDTO {

    @NotBlank(message = "Token is required")
    private String token;
}
