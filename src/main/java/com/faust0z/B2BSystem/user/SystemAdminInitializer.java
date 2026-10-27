package com.faust0z.B2BSystem.user;

import com.faust0z.B2BSystem.user.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.faust0z.B2BSystem.user.Role;
import com.faust0z.B2BSystem.user.SystemAdminInitializer;
import com.faust0z.B2BSystem.user.User;
import com.faust0z.B2BSystem.user.UserRepository;
@Slf4j
@Component
public class SystemAdminInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    SystemAdminInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email}") String adminEmail,
            @Value("${app.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeSystemAdmin() {
        if (!userRepository.existsByEmail(adminEmail)) {
            log.info("System admin not found. Bootstrapping initial admin user...");

            User admin = new User();
            admin.setFullName("Administrador Global");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            admin.setUsername("admin_test");
            admin.setIsActive(true);

            userRepository.save(admin);
            log.info("Initial admin user bootstrapped successfully.");
        }
    }
}