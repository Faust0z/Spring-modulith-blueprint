package com.faust0z.B2BSystem.contacts;

import com.faust0z.B2BSystem.access.domain.ScanType;
import com.faust0z.B2BSystem.contacts.domain.ContactAccessInternalResponse;
import com.faust0z.B2BSystem.contacts.domain.ContactInternalStats;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ContactInternalApi {
    ContactAccessInternalResponse findActiveContact(String identifier, ScanType scanType);

    ContactInternalStats getContactStats(Instant currentTime);

    UUID validateContactActive(UUID contactId);

    int countContactsWithAccess(Instant referenceDate);

    ContactAccessInternalResponse findActiveContactByDni(String dni);

    Optional<Boolean> getContactStatus(String dni);
}
