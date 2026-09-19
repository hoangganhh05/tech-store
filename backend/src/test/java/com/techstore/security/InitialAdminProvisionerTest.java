package com.techstore.security;

import com.techstore.entity.Role;
import com.techstore.entity.User;
import com.techstore.enums.RoleCode;
import com.techstore.repository.RoleRepository;
import com.techstore.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitialAdminProvisionerTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;

    @Test
    void provisionsExplicitAdministratorOnlyWhenTheEmailDoesNotExist() throws Exception {
        InitialAdminProperties properties = properties();
        Role adminRole = new Role(RoleCode.ADMIN, "Quản trị viên");
        when(userRepository.existsByEmailIgnoreCase("operations-admin@example.com")).thenReturn(false);
        when(roleRepository.findByCode(RoleCode.ADMIN)).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode("initial-admin-password-2026")).thenReturn("bcrypt-hash");

        provisioner(properties).run(new DefaultApplicationArguments());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("operations-admin@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(saved.getRoleCodes()).containsExactly(RoleCode.ADMIN);
    }

    @Test
    void doesNotResetOrOverwriteAnExistingAdministrator() throws Exception {
        InitialAdminProperties properties = properties();
        when(userRepository.existsByEmailIgnoreCase("operations-admin@example.com")).thenReturn(true);

        provisioner(properties).run(new DefaultApplicationArguments());

        verify(roleRepository, never()).findByCode(RoleCode.ADMIN);
        verify(passwordEncoder, never()).encode("initial-admin-password-2026");
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private InitialAdminProvisioner provisioner(InitialAdminProperties properties) {
        return new InitialAdminProvisioner(properties, userRepository, roleRepository, passwordEncoder);
    }

    private InitialAdminProperties properties() {
        InitialAdminProperties properties = new InitialAdminProperties();
        properties.setEmail("operations-admin@example.com");
        properties.setPassword("initial-admin-password-2026");
        properties.setFullName("Operations Administrator");
        return properties;
    }
}
