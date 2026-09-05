package com.techstore.repository;

import com.techstore.entity.PasswordResetToken;
import com.techstore.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PasswordResetToken token set token.usedAt = :usedAt "
            + "where token.user = :user and token.usedAt is null")
    int markUnusedTokensAsUsed(@Param("user") User user, @Param("usedAt") Instant usedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PasswordResetToken token set token.usedAt = :usedAt "
            + "where token.tokenHash = :tokenHash and token.usedAt is null and token.expiresAt > :now")
    int claimUsableToken(
            @Param("tokenHash") String tokenHash,
            @Param("now") Instant now,
            @Param("usedAt") Instant usedAt
    );
}
