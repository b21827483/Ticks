package com.ticks.user_service.service;

import com.ticks.user_service.dto.request.*;
import com.ticks.user_service.dto.response.AuthResponseDTO;

public interface AuthService {
    AuthResponseDTO register(RegisterRequestDTO request);
    AuthResponseDTO login(LoginRequestDTO request);
    AuthResponseDTO refreshToken(RefreshTokenRequestDTO request);
    void forgotPassword(ForgotPasswordRequestDTO request);
    void resetPassword(ResetPasswordRequestDTO request);
    void logout(String accessToken, String refreshToken);
}
