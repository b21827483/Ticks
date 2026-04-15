package com.ticks.user_service.controller;

import com.ticks.user_service.dto.request.ChangePasswordRequestDTO;
import com.ticks.user_service.dto.response.ApiResponseDTO;
import com.ticks.user_service.dto.response.UserResponseDTO;
import com.ticks.user_service.entity.Role;
import com.ticks.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        UserResponseDTO user = userService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponseDTO.success("User retrieved", user));
    }

    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequestDTO request) {

        userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponseDTO.success("Password changed successfully"));
    }

    @GetMapping("/{id}/")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> getUserById(@PathVariable UUID uuid) {

        UserResponseDTO user = userService.getUserById(uuid);
        return  ResponseEntity.ok(ApiResponseDTO.success("User retrieved", user));
    }

    @PostMapping("/{id}/roles/{role}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> assignRole(
            @PathVariable UUID id, @PathVariable Role role) {
        UserResponseDTO user = userService.assignRole(id, role);
        return ResponseEntity.ok(ApiResponseDTO.success("Role assigned", user));
    }

    @DeleteMapping("{id}/roles/{role}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> removeRole(
            @PathVariable UUID id, @PathVariable Role role) {

        UserResponseDTO user = userService.removeRole(id, role);
        return ResponseEntity.ok(ApiResponseDTO.success("Role removed", user));
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponseDTO<Void>> suspendUser(@PathVariable UUID id) {
        userService.suspendUser(id);
        return ResponseEntity.ok(ApiResponseDTO.success("User suspended"));
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponseDTO<Void>> reactivateUser(@PathVariable UUID id) {
        userService.reactivateUser(id);
        return ResponseEntity.ok(ApiResponseDTO.success("User reactivated"));
    }
}
