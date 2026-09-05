package com.techstore.security;

import com.techstore.entity.User;

public interface TokenIssuer {

    IssuedTokenPair issue(User user);

    Long getAccessTokenUserId(String accessToken);

    String getRefreshTokenId(String refreshToken);
}
