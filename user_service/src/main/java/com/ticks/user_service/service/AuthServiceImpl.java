package com.ticks.user_service.service;

import com.ticks.user_service.dto.request.*;
import com.ticks.user_service.dto.response.AuthResponseDTO;
import com.ticks.user_service.dto.response.UserResponseDTO;
import com.ticks.user_service.entity.*;
import com.ticks.user_service.exception.AccountLockedException;
import com.ticks.user_service.exception.InvalidTokenException;
import com.ticks.user_service.exception.UserAlreadyExistsException;
import com.ticks.user_service.exception.UserNotFoundException;
import com.ticks.user_service.kafka.UserEventProducer;
import com.ticks.user_service.repository.TokenRepository;
import com.ticks.user_service.repository.UserRepository;
import com.ticks.user_service.security.JwtTokenProvider;
import com.ticks.user_service.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserEventProducer userEventProducer;
    private final RedisTokenService redisTokenService;
    private final EmailVerificationService emailVerificationService;

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
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .userStatus(UserStatus.PENDING)
                .roles(Set.of(Role.ROLE_USER))
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());

        userEventProducer.publishUserRegistered(user);

        emailVerificationService.sendVerificationEmail(user.getId());

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        String refreshToken = createRefreshToken(user, userDetails);
        return authResponseBuilder(accessToken, refreshToken, user);
    }

    @Override
    @Transactional
    public AuthResponseDTO login(LoginRequestDTO request) {

        long attempts = redisTokenService.getLoginAttempts(request.getEmail());
        if (attempts >= 4) {
            throw new AccountLockedException(
                    "Too many failed attempts. Please try again in 15 minutes."
            );
        }

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
            redisTokenService.incrementLoginAttempt(request.getEmail());
            throw e;
        }

        user.resetFailedAttempts();
        userRepository.updateLastLoginAt(user.getId(), LocalDateTime.now());
        userRepository.save(user);

        redisTokenService.resetLoginAttempts(request.getEmail());

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
    @Transactional
    public void forgotPassword(ForgotPasswordRequestDTO request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(
                user -> {
                    tokenRepository.revokeAllUserTokensByType(user.getId(), TokenType.PASSWORD_RESET);

                    redisTokenService.evictResetToken(request.getEmail());

                    String rawToken = UUID.randomUUID().toString();
                    LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);

                    Token resetToken = Token.builder()
                            .token(rawToken)
                            .tokenType(TokenType.PASSWORD_RESET)
                            .user(user)
                            .createdAt(LocalDateTime.now())
                            .expiresAt(expiresAt)
                            .build();
                    tokenRepository.save(resetToken);

                    redisTokenService.cacheResetToken(rawToken, user.getEmail());

                    userEventProducer.publishPasswordResetRequested(user, rawToken, expiresAt);
                    log.info("Password reset token issued for: {}", user.getEmail());
                });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequestDTO request) {

        Token resetToken = tokenRepository
                .findByTokenAndTokenType(request.getToken(), TokenType.PASSWORD_RESET)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired password reset token"));

        if (!resetToken.isValid()) {
            redisTokenService.evictResetToken(request.getToken());
            throw new InvalidTokenException("Password reset token has expired or already been used");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        resetToken.setRevoked(true);
        tokenRepository.save(resetToken);
        tokenRepository.revokeAllUserTokensByType(user.getId(), TokenType.REFRESH_TOKEN);

        redisTokenService.evictResetToken(request.getToken());

        userEventProducer.publishPasswordChanged(user);

        log.info("Password reset completed for: {}", user.getEmail());
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
                        jwtTokenProvider.getRefreshTokenExpirationMs() / 1000))
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
