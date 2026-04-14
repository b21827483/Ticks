package com.ticks.user_service.service;

import com.ticks.user_service.dto.request.*;
import com.ticks.user_service.dto.response.AuthResponseDTO;
import com.ticks.user_service.dto.response.UserResponseDTO;
import com.ticks.user_service.entity.*;
import com.ticks.user_service.exception.AccountLockedException;
import com.ticks.user_service.exception.InvalidTokenException;
import com.ticks.user_service.exception.UserAlreadyExistsException;
import com.ticks.user_service.exception.UserNotFoundException;
import com.ticks.user_service.repository.TokenRepository;
import com.ticks.user_service.repository.UserRepository;
import com.ticks.user_service.security.JwtTokenProvider;
import com.ticks.user_service.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already taken " + request.getUsername());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(request.getPassword())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .userStatus(UserStatus.PENDING)
                .roles(Set.of(Role.ROLE_USER))
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        String refreshToken = createRefreshToken(user, userDetails);
        return authResponseBuilder(accessToken, refreshToken, user);
    }

    @Override
    @Transactional
    public AuthResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(
                () -> new UserNotFoundException("User not found"));

        if (user.isAccountLocked()) {
            throw new AccountLockedException("Account is temporarily locked. Try again after " + user.getLockedUntil());
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (Exception e) {
            user.incrementFailedAttempts();
            userRepository.save(user);
            throw e;
        }

        user.resetFailedAttempts();
        userRepository.updateLastLoginAt(user.getId(), LocalDateTime.now());
        userRepository.save(user);

        tokenRepository.revokeAllUserTokensByType(user.getId(), TokenType.REFRESH_TOKEN);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        String refreshToken = createRefreshToken(user, userDetails);

        log.info("user logged in: {}", user.getEmail());
        return authResponseBuilder(accessToken, refreshToken, user);
    }

    @Override
    @Transactional
    public AuthResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        Token storedToken = tokenRepository.findByTokenAndTokenType(request.getRefreshToken(), TokenType.REFRESH_TOKEN)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (!storedToken.isValid()) {
            throw new InvalidTokenException("Refresh token is expired or revoked");
        }

        User user = storedToken.getUser();

        storedToken.setRevoked(true);
        tokenRepository.save(storedToken);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        String refreshToken = createRefreshToken(user, userDetails);

        return authResponseBuilder(accessToken, refreshToken, user);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequestDTO request) {

    }

    @Override
    public void resetPassword(ResetPasswordRequestDTO request) {

    }

    @Override
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        if (refreshToken != null) {
            tokenRepository.findByTokenAndTokenType(refreshToken, TokenType.REFRESH_TOKEN)
                    .ifPresent(token -> {
                            token.setRevoked(true);
                            tokenRepository.save(token);
                    });
        }

        log.info("User logged out successfully");
    }

    private AuthResponseDTO authResponseBuilder(String accessToken, String refreshToken, User user) {
        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer ")
                .expiresIn(900)
                .user(mapToUserResponseDTO(user))
                .build();

    }

    private String createRefreshToken(User user, UserDetails userDetails) {
        String rawRefreshToken = jwtTokenProvider.generateRefreshToken(userDetails);
        Token refreshToken = Token.builder()
                .token(rawRefreshToken)
                .tokenType(TokenType.REFRESH_TOKEN)
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(
                        jwtTokenProvider.getRefreshTokenExpirationMs() / 100))
                .build();
        tokenRepository.save(refreshToken);
        return rawRefreshToken;
    }

    private UserResponseDTO mapToUserResponseDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .userStatus(user.getUserStatus())
                .roles(user.getRoles())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
