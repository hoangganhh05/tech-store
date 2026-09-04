package com.techstore.service;

import com.techstore.dto.request.RegisterRequest;
import com.techstore.dto.response.UserResponse;
import com.techstore.entity.Role;
import com.techstore.entity.User;
import com.techstore.enums.RoleCode;
import com.techstore.exception.BusinessException;
import com.techstore.mapper.UserMapper;
import com.techstore.repository.RoleRepository;
import com.techstore.repository.UserRepository;
import com.techstore.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthServiceImpl authService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, roleRepository, passwordEncoder, new UserMapper());
    }

    @Test
    void registerNormalizesEmailHashesPasswordAndAssignsCustomerRole() {
        RegisterRequest request = request("  Customer@Example.com ");
        Role customerRole = new Role(RoleCode.CUSTOMER, "Customer");
        User savedUser = new User("customer@example.com", "encoded-password", "Nguyen Van A", "0901234567");
        savedUser.addRole(customerRole);

        when(userRepository.existsByEmailIgnoreCase("customer@example.com")).thenReturn(false);
        when(roleRepository.findByCode(RoleCode.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("strong-password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse actual = authService.register(request);

        assertThat(actual.email()).isEqualTo("customer@example.com");
        assertThat(actual.fullName()).isEqualTo("Nguyen Van A");
        assertThat(actual.roles()).containsExactly("CUSTOMER");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User user = userCaptor.getValue();
        assertThat(user.getEmail()).isEqualTo("customer@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(user.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(user.getPhone()).isEqualTo("0901234567");
        assertThat(user.getRoles()).extracting(Role::getCode).containsExactly(RoleCode.CUSTOMER);
    }

    @Test
    void registerRejectsAnExistingEmailBeforeEncodingPassword() {
        when(userRepository.existsByEmailIgnoreCase("customer@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request("customer@example.com")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Email đã được đăng ký");

        verify(passwordEncoder, never()).encode(any());
        verify(roleRepository, never()).findByCode(any());
        verify(userRepository, never()).save(any());
    }

    private RegisterRequest request(String email) {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Nguyen Van A");
        request.setEmail(email);
        request.setPhone("0901234567");
        request.setPassword("strong-password");
        request.setConfirmPassword("strong-password");
        return request;
    }
}
