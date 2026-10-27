package com.faust0z.B2BSystem.contacts.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TenantId;

import com.faust0z.B2BSystem.contacts.web.ContactResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "v_member_details")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContactDetailsView {
    @Id
    private UUID id;
    @TenantId
    private UUID tenantId;
    private String dni;
    private String firstName;
    private String lastName;
    private String currentMembershipName;
    private Instant accessUntil;
    private UUID accessToken;
    private Boolean isActive;
    private Instant createdAt;
    private String email;
    private LocalDate birthDate;
    private String phoneNumber;
    @Enumerated(EnumType.STRING)
    private GenderCategory gender;
    private Instant lastScannedAt;
    private Integer debtAmount;

    public ContactResponse toResponse() {
        String fullName = firstName + " " + lastName;
        boolean canAccess = Boolean.TRUE.equals(isActive) && accessUntil != null && accessUntil.isAfter(Instant.now());
        String currMembershipName = (canAccess && currentMembershipName != null) ? currentMembershipName : "Sin membresía";

        return new ContactResponse(id, dni, firstName, lastName, fullName, currMembershipName, accessUntil, canAccess, accessToken,
                isActive, createdAt, email, birthDate, phoneNumber, gender, lastScannedAt, debtAmount);
    }
}
