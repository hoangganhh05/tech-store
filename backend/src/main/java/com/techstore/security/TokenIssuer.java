package com.techstore.security;

import com.techstore.entity.User;

public interface TokenIssuer {

    IssuedTokenPair issue(User user);
}
