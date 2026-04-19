package com.ticks.user_service.service;

import com.ticks.user_service.entity.Token;
import com.ticks.user_service.entity.TokenType;
import com.ticks.user_service.entity.User;
import com.ticks.user_service.exception.UserNotFoundException;
import com.ticks.user_service.kafka.UserEventProducer;
import com.ticks.user_service.repository.TokenRepository;
import com.ticks.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final RedisTokenService redisTokenService;
    private final UserEventProducer userEventProducer;

    @Value("${app.email-verification.expiration-minutes:1440}") // 24 hours default
    private long expirationMinutes;

    @Transactional
    public void sendVerificationEmail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        issueAndPublish(user);
    }

    private void issueAndPublish(User user) {
        tokenRepository.revokeAllUserTokensByType(user.getId(), TokenType.EMAIL_VERIFICATION);

        String rawToken = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);

        Token verificationToken = Token.builder()
                .token(rawToken)
                .tokenType(TokenType.EMAIL_VERIFICATION)
                .user(user)
                .expiresAt(expiresAt)
                .build();
        tokenRepository.save(verificationToken);

        redisTokenService.cacheVerificationToken(
                rawToken, user.getId().toString(), expiresAt
        );

        userEventProducer.publishEmailVerificationRequested(user, rawToken, expiresAt);

        log.info("Verification token issued for {} (expires {})", user.getEmail(), expiresAt);
    }
}

