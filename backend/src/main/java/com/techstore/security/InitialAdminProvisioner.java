package com.techstore.security;

import com.techstore.entity.Role;
import com.techstore.entity.User;
import com.techstore.enums.RoleCode;
import com.techstore.repository.RoleRepository;
import com.techstore.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Creates an administrator only when an explicit deployment secret is supplied.
 * Production validation rejects blank or unsafe values before this runner executes.
 */
@Component
@Profile("prod")
public class InitialAdminProvisioner implements ApplicationRunner {

    private final InitialAdminProperties properties;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public InitialAdminProvisioner(
            InitialAdminProperties properties,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = trimToNull(properties.getEmail());
        String password = properties.getPassword();
        if (email == null || password == null || password.isBlank()) {
            return;
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }

        Role adminRole = roleRepository.findByCode(RoleCode.ADMIN)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy vai trò ADMIN để khởi tạo tài khoản quản trị"));
        String fullName = trimToNull(properties.getFullName());
        User admin = new User(
                email.toLowerCase(Locale.ROOT),
                passwordEncoder.encode(password),
                fullName == null ? "Quản trị viên hệ thống" : fullName,
                trimToNull(properties.getPhone())
        );
        admin.addRole(adminRole);
        userRepository.save(admin);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
