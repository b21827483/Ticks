package com.ticks.user_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {
    private UUID id;
    private String email;
    private String username;
    private String firstName;
    private LocalDateTime registeredAt;
}
