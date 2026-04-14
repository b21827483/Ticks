package com.ticks.user_service.repository;

import com.ticks.user_service.entity.Token;
import com.ticks.user_service.entity.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface TokenRepository extends JpaRepository<Token, UUID> {

    Optional<Token> findByTokenAndTokenType(String token, TokenType tokenType);

    @Modifying
    @Query("UPDATE Token t SET t.revoked = true WHERE t.id = :userId AND t.tokenType = :tokenType AND t.revoked = false")
    void revokeAllUserTokensByType(@Param("userId") UUID id, @Param("tokenType") TokenType tokenType);

    @Modifying
    @Query("DELETE FROM Token t WHERE t.expiresAt < :now")
    void deleteExpiredTokens(@Param("now")LocalDateTime now);
}
