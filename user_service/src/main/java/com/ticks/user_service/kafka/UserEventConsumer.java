package com.ticks.user_service.kafka;

import com.ticks.user_service.entity.UserStatus;
import com.ticks.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserEventConsumer {

    private final UserRepository userRepository;

    @KafkaListener(
            topics = "${app.kafka.topics.payment-completed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentCompleted(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received payment.completed from topic={} partition={} offset={}",
                topic, partition, offset);

        try {
            UUID userId = UUID.fromString(payload.get("userId").toString());
            userRepository.findById(userId).ifPresent(user -> {
                if (user.getUserStatus() == UserStatus.PENDING) {
                    userRepository.updateStatus(userId, UserStatus.ACTIVE);
                    log.info("User {} activated after payment: ", userId);
                }
            });
        } catch (Exception e) {
            log.error("Failed to process payment.completed event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(
            topics = "${app.kafka.topics.email-verified}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onEmailVerified(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        log.info("Received email.verified from topic={}", topic);

        try {
            UUID userId = UUID.fromString(payload.get("userId").toString());
            userRepository.findById(userId).ifPresent(user -> {
                if (user.getUserStatus() == UserStatus.PENDING) {
                    userRepository.updateStatus(userId, UserStatus.ACTIVE);
                    log.info("User {} activated after email verification", userId);
                }
            });
        } catch (Exception e) {
            log.error("Failed to process email.verified event: {}", e.getMessage(), e);
        }
    }
}
