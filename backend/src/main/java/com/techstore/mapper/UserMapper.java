package com.techstore.mapper;

import com.techstore.entity.User;
import com.techstore.dto.response.UserResponse;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getStatus().name(),
                user.getRoles().stream()
                        .map(role -> role.getCode().name())
                        .collect(Collectors.toUnmodifiableSet()),
                user.isEmailVerified(),
                user.getCreatedAt()
        );
    }
}
