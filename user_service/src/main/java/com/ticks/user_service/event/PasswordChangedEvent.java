package com.ticks.user_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PasswordChangedEvent {
    private UUID id;
    private String email;
    private String firstName;
    private LocalDateTime changedAt;
}
