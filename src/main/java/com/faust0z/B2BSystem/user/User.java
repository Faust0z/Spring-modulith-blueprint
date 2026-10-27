package com.faust0z.B2BSystem.user;

import com.faust0z.B2BSystem.core.SystemMutableEntity;
import com.faust0z.B2BSystem.user.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends SystemMutableEntity {
    @Column(name = "tenant_id", updatable = false)
    private UUID tenantId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "username", unique = true, length = 50)
    private String username;

    @Column(name = "password")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private Role role = Role.TENANT_USER;

    @Column(name = "terms_accepted_at")
    private Instant termsAcceptedAt;

    @Column(name = "admin_pin")
    private String adminPin;
}
