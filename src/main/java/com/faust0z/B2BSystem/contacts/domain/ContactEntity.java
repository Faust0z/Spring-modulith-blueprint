package com.faust0z.B2BSystem.contacts.domain;

import com.faust0z.B2BSystem.core.TenantSoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "members")
public class ContactEntity extends TenantSoftDeletableEntity {

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "dni", nullable = false, length = 20)
    private String dni;

    @Column(name = "qr_token", nullable = false, unique = true)
    private UUID accessToken;

    @Column(name = "access_until")
    private Instant accessUntil;

    @Column(name = "current_membership_name")
    private String currentMembershipName;

    @Column(name = "email")
    private String email;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 50)
    private GenderCategory gender;

    @Column(name = "debt_amount", nullable = false)
    private int debtAmount = 0;
}
