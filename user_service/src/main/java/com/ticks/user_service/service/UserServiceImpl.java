package com.ticks.user_service.service;

import com.ticks.user_service.dto.request.ChangePasswordRequestDTO;
import com.ticks.user_service.dto.response.UserResponseDTO;
import com.ticks.user_service.entity.Role;
import com.ticks.user_service.entity.TokenType;
import com.ticks.user_service.entity.User;
import com.ticks.user_service.entity.UserStatus;
import com.ticks.user_service.exception.UserNotFoundException;
import com.ticks.user_service.repository.TokenRepository;
import com.ticks.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    @Transactional
    public UserResponseDTO getCurrentUser(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new UserNotFoundException("User not found with email: " + email));
        return mapToResponse(user);
    }

    @Override
    @Transactional
    public UserResponseDTO getUserById(UUID id) {
        User user = userRepository.findById(id).orElseThrow(
                () -> new UserNotFoundException("User not found with id " + id));
        return mapToResponse(user);
    }

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequestDTO request) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new UserNotFoundException("User not found with email: " + email));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        tokenRepository.revokeAllUserTokensByType(user.getId(), TokenType.REFRESH_TOKEN);
        log.info("Password changed for user {}", email);
    }

    @Override
    @Transactional
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
        log.info("User suspended: {}", userId);
    }

    @Override
    public void reactivateUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("User not found with id: " + userId));
        userRepository.updateStatus(userId, UserStatus.ACTIVE);
        log.info("User reactivated: {}", userId);
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
