package com.ticks.user_service.kafka;

import com.ticks.user_service.entity.User;
import com.ticks.user_service.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cglib.core.Local;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.user-registered}")
    private String userRegisteredTopic;

    @Value("${app.kafka.topics.password-reset-requested}")
    private String passwordResetTopic;

    @Value("${app.kafka.topics.password-changed}")
    private String passwordChangedTopic;

    @Value("${app.kafka.topics.email-verification-requested}")
    private String emailVerificationRequestedTopic;

    @Value("${app.kafka.topics.email-verified}")
    private String emailVerifiedTopic;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Value("${app.frontend.reset-password-path}")
    private String resetPasswordPath;

    public void publishUserRegistered(User user) {
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .registeredAt(LocalDateTime.now())
                .build();
        send(userRegisteredTopic, user.getId().toString(), event);
    }

    public void publishPasswordResetRequested(User user, String rawToken) {
        String resetLink = frontendBaseUrl + resetPasswordPath + "?token=" + rawToken;
        PasswordResetRequestEvent event = PasswordResetRequestEvent.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .resetToken(rawToken)
                .resetLink(resetLink)
                .requestedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
        send(passwordResetTopic, user.getId().toString(), event);
    }

    public void publishPasswordChanged(User user) {
        PasswordChangedEvent event = PasswordChangedEvent.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .changedAt(LocalDateTime.now())
                .build();
        send(passwordChangedTopic, user.getId().toString(), event);
    }

    public void publishEmailVerificationRequested(
            User user, String rawToken, LocalDateTime expiresAt) {

        send(emailVerificationRequestedTopic, user.getId().toString(),
                EmailVerificationRequestedEvent.builder()
                        .userId(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .verificationToken(rawToken)
                        .requestedAt(LocalDateTime.now())
                        .expiresAt(expiresAt)
                        .build());
    }

    public void publishEmailVerified(User user) {
        send(emailVerifiedTopic, user.getId().toString(),
                EmailVerifiedEvent.builder()
                        .userId(user.getId())
                        .email(user.getEmail())
                        .username(user.getUsername())
                        .verifiedAt(LocalDateTime.now())
                        .build());
    }

    private void send(String topic, String key, Object payload) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key, payload);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event to topic [{}] key [{}]: {}",
                        topic, key, ex.getMessage(), ex);
            } else {
                log.debug("Published event to topic [{}] partition [{}] offset [{}]",
                        topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });
    }
}
