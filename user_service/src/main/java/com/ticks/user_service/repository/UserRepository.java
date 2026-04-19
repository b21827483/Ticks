package com.ticks.user_service.repository;

import com.ticks.user_service.config.CacheNames;
import com.ticks.user_service.entity.User;
import com.ticks.user_service.entity.UserStatus;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    @Cacheable(
            value = CacheNames.USER_EXISTS_BY_EMAIL,
            key = "#email",
            unless = "!#result"
    )
    boolean existsByEmail(String email);

    @Cacheable(
            value = CacheNames.USERS_EXISTS_BY_USERNAME,
            key = "#username",
            unless = "!#result"
    )
    boolean existsByUsername(String username);

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :lastLoginAt WHERE u.id = :id")
    @CacheEvict(
            value = CacheNames.USER_BY_ID,
            key = "#id.toString()"
    )
    void updateLastLoginAt(@Param("id") UUID id, @Param("lastLoginAt") LocalDateTime lastLoginAt);

    @Modifying
    @Query("UPDATE User u SET u.userStatus = :userStatus WHERE u.id = :id")
    @CacheEvict(
            value = CacheNames.USER_BY_ID,
            key = "#id.toString()"
    )
    void updateStatus(@Param("id") UUID id, UserStatus userStatus);
}
