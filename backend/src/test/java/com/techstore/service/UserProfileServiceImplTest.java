package com.techstore.service;

import com.techstore.dto.request.UpdateProfileRequest;
import com.techstore.dto.response.UserProfileResponse;
import com.techstore.entity.User;
import com.techstore.enums.ErrorCode;
import com.techstore.enums.UserStatus;
import com.techstore.exception.BusinessException;
import com.techstore.repository.UserRepository;
import com.techstore.service.impl.UserProfileServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void getProfileReturnsPersonalInformationWithoutPassword() {
        User user = new User("customer@example.com", "encoded-password", "Nguyen Van A", "0901234567");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse response = new UserProfileServiceImpl(userRepository).getProfile(1L);

        assertThat(response.email()).isEqualTo("customer@example.com");
        assertThat(response.fullName()).isEqualTo("Nguyen Van A");
        assertThat(response.phone()).isEqualTo("0901234567");
    }

    @Test
    void updateProfileChangesAllowedFieldsAndKeepsEmail() {
        User user = new User("customer@example.com", "encoded-password", "Old Name", "0901234567");
        UpdateProfileRequest request = request("New Name", "0987654321", LocalDate.of(2000, 5, 20));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserProfileResponse response = new UserProfileServiceImpl(userRepository).updateProfile(1L, request);

        assertThat(response.email()).isEqualTo("customer@example.com");
        assertThat(response.fullName()).isEqualTo("New Name");
        assertThat(response.phone()).isEqualTo("0987654321");
        assertThat(response.dateOfBirth()).isEqualTo(LocalDate.of(2000, 5, 20));
        verify(userRepository).save(user);
    }

    @Test
    void profileRejectsAUserMissingFromTheAccessTokenSubject() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException exception = catchThrowableOfType(
                () -> new UserProfileServiceImpl(userRepository).getProfile(999L),
                BusinessException.class
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_ACCESS_TOKEN);
    }

    @Test
    void profileRejectsALockedAccount() {
        User user = new User("locked@example.com", "encoded-password", "Locked User", "0901234567");
        user.changeStatus(UserStatus.LOCKED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        BusinessException exception = catchThrowableOfType(
                () -> new UserProfileServiceImpl(userRepository).getProfile(1L),
                BusinessException.class
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_LOCKED);
    }

    private UpdateProfileRequest request(String fullName, String phone, LocalDate dateOfBirth) {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName(fullName);
        request.setPhone(phone);
        request.setDateOfBirth(dateOfBirth);
        return request;
    }
}
