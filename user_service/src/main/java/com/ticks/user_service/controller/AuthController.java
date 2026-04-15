package com.ticks.user_service.controller;

import com.ticks.user_service.dto.request.*;
import com.ticks.user_service.dto.response.ApiResponseDTO;
import com.ticks.user_service.dto.response.AuthResponseDTO;
import com.ticks.user_service.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Register, login, token refresh, and password management")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponseDTO<AuthResponseDTO>> register(
            @Valid @RequestBody RegisterRequestDTO request) {
        AuthResponseDTO authResponse = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success("Registration successful.", authResponse));
    }

    @PostMapping("/login")
    @Operation(summary = "Login user")
    public ResponseEntity<ApiResponseDTO<AuthResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO request) {
        AuthResponseDTO authResponse = authService.login(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseDTO.success("Login successful", authResponse));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user")
    public ResponseEntity<ApiResponseDTO<Void>> logout(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody(required = false) RefreshTokenRequestDTO request) {

        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }

        String refreshToken = (request != null) ? request.getRefreshToken() : null;
        authService.logout(accessToken, refreshToken);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseDTO.success("Logged out successfully"));
    }

    @PostMapping
    @Operation(summary = "Refresh token")
    public ResponseEntity<ApiResponseDTO<AuthResponseDTO>> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDTO request) {

        AuthResponseDTO authResponse = authService.refreshToken(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseDTO.success("Token refreshed", authResponse));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Handle forgot password")
    public ResponseEntity<ApiResponseDTO<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO request) {
        authService.forgotPassword(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseDTO.success("If given email exists, a reset link has been sent"));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Handle change password")
    public ResponseEntity<ApiResponseDTO<Void>> changePassword(
            @Valid @RequestBody ResetPasswordRequestDTO request) {

        authService.resetPassword(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponseDTO.success("Password reset successfully"));
    }
}
