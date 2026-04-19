package com.ticks.user_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticks.user_service.config.CacheNames;
import com.ticks.user_service.dto.request.ChangePasswordRequestDTO;
import com.ticks.user_service.dto.response.UserResponseDTO;
import com.ticks.user_service.entity.Role;
import com.ticks.user_service.entity.TokenType;
import com.ticks.user_service.entity.User;
import com.ticks.user_service.entity.UserStatus;
import com.ticks.user_service.exception.UserNotFoundException;
import com.ticks.user_service.kafka.UserEventProducer;
import com.ticks.user_service.repository.TokenRepository;
import com.ticks.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserEventProducer userEventProducer;
    private final RedisTokenService redisTokenService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
        value = CacheNames.USER_BY_EMAIL,
        key = "#email",
        unless = "#result == null"
    )
    public UserResponseDTO getCurrentUser(String email) {
        String cached = redisTokenService.getCachedUserProfile(email);
        if (cached != null) {
            try {
                log.debug("Profile string cache HIT for: {}", email);
                return objectMapper.readValue(cached, UserResponseDTO.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize cached profile for {}, falling through to DB", email);
            }
        }
        log.debug("Cache MISS - loading user by email: {}", email);
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new UserNotFoundException("User not found with email: " + email));

        UserResponseDTO dto = mapToResponse(user);
        try {
            redisTokenService.cacheUserProfile(email, objectMapper.writeValueAsString(dto));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize user profile for caching: {}", e.getMessage());
        }

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            value = CacheNames.USER_BY_ID,
            key = "#id.toString()",
            unless = "#result == null"
    )
    public UserResponseDTO getUserById(UUID id) {
        log.debug("Cache MISS - loading user by id: {}", id);
        User user = userRepository.findById(id).orElseThrow(
                () -> new UserNotFoundException("User not found with id " + id));
        return mapToResponse(user);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.USER_BY_EMAIL, key = "#email"),
            @CacheEvict(value = CacheNames.USER_EXISTS_BY_EMAIL, key = "#email")
    })
    public void changePassword(String email, ChangePasswordRequestDTO request) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new UserNotFoundException("User not found with email: " + email));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        tokenRepository.revokeAllUserTokensByType(user.getId(), TokenType.REFRESH_TOKEN);

        evictById(user.getId());
        redisTokenService.evictUserProfile(email);

        userEventProducer.publishPasswordChanged(user);
        log.info("Password changed for user {}", email);
    }

    @Override
    @Transactional
    @Caching(put = {
            @CachePut(value = CacheNames.USER_BY_EMAIL, key = "#result.email"),
            @CachePut(value = CacheNames.USER_BY_ID, key = "#result.id.toString()")
    })
    public UserResponseDTO assignRole(UUID userId, Role role) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("User not found with id: " + userId));
        if (user.getRoles().contains(role)) {
            log.info("User already has this role: {}", role);
        } else {
            user.getRoles().add(role);
            userRepository.save(user);
            log.info("Role {} assigned to user: {}", role, userId);
        }
        return mapToResponse(user);
    }

    @Override
    @Transactional
    @Caching(put = {
            @CachePut(value = CacheNames.USER_BY_EMAIL, key = "#result.email"),
            @CachePut(value = CacheNames.USER_BY_ID, key = "#result.id.toString()")
    })
    public UserResponseDTO removeRole(UUID userId, Role role) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("User not found with id: " + userId));
        if (user.getRoles().contains(role)) {
            user.getRoles().remove(role);
            userRepository.save(user);
            log.info("Role {} removed from user: {}", role, userId);
        } else {
            log.info("User: {} doesn't have this role: {}", userId, role);
        }
        return mapToResponse(user);
    }

    @Override
    @Transactional
    public void suspendUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("User not found with id: " + userId));
        userRepository.updateStatus(userId, UserStatus.SUSPENDED);
        tokenRepository.revokeAllUserTokensByType(userId, TokenType.REFRESH_TOKEN);

        evictBothRegions(userId, user.getEmail());
        redisTokenService.evictUserProfile(user.getEmail());
        log.info("User suspended: {}", userId);
    }

    @Override
    public void reactivateUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("User not found with id: " + userId));
        userRepository.updateStatus(userId, UserStatus.ACTIVE);
        evictBothRegions(userId, user.getEmail());
        redisTokenService.evictUserProfile(user.getEmail());
        log.info("User reactivated: {}", userId);
    }

    @CacheEvict(value = CacheNames.USER_BY_ID, key = "#userId.toString()")
    private void evictById(UUID userId) {
        // Annotation does the work
    }

    @CacheEvict(value = CacheNames.USER_BY_EMAIL, key = "#email")
    private void evictByEmail(String email) {
        // Annotation does the work
    }

    private void evictBothRegions(UUID userId, String email) {
        evictByEmail(email);
        evictById(userId);
    }

    public UserResponseDTO mapToResponse(User user) {
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
