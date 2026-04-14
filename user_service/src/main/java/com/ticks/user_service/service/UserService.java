package com.ticks.user_service.service;

import com.ticks.user_service.dto.request.ChangePasswordRequestDTO;
import com.ticks.user_service.dto.response.UserResponseDTO;
import com.ticks.user_service.entity.Role;

import java.util.UUID;

public interface UserService {
    UserResponseDTO getCurrentUser(String email);
    UserResponseDTO getUserById(UUID id);
    void changePassword(String email, ChangePasswordRequestDTO request);

    UserResponseDTO assignRole(UUID userId, Role role);
    UserResponseDTO removeRole(UUID userId, Role role);
    void suspendUser(UUID userId);
    void reactivateUser(UUID userId);
}
