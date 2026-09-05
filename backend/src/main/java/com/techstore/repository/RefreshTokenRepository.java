package com.techstore.repository;

import com.techstore.entity.RefreshToken;
import com.techstore.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenId(String tokenId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken token set token.revokedAt = :revokedAt "
            + "where token.user = :user and token.revokedAt is null")
    int revokeActiveTokensByUser(@Param("user") User user, @Param("revokedAt") Instant revokedAt);
}
