package com.faust0z.B2BSystem.contacts.application;

import com.faust0z.B2BSystem.core.DomainRuleException;
import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.access.domain.ScanType;
import com.faust0z.B2BSystem.contacts.ContactInternalApi;
import com.faust0z.B2BSystem.contacts.domain.ContactAccessInternalResponse;
import com.faust0z.B2BSystem.contacts.domain.ContactEntity;
import com.faust0z.B2BSystem.contacts.domain.ContactInternalStats;
import com.faust0z.B2BSystem.contacts.infrastructure.ContactDetailsViewRepository;
import com.faust0z.B2BSystem.contacts.infrastructure.ContactRepository;
import com.faust0z.B2BSystem.contacts.web.ContactCreateRequest;
import com.faust0z.B2BSystem.contacts.web.ContactDisabledException;
import com.faust0z.B2BSystem.contacts.web.ContactDniAlreadyRegisteredException;
import com.faust0z.B2BSystem.contacts.web.ContactFilterRequest;
import com.faust0z.B2BSystem.contacts.web.ContactImportRequest;
import com.faust0z.B2BSystem.contacts.web.ContactNotFoundException;
import com.faust0z.B2BSystem.contacts.web.ContactQuickStatsResponse;
import com.faust0z.B2BSystem.contacts.web.ContactResponse;
import com.faust0z.B2BSystem.contacts.web.ContactUpdateRequest;
import com.faust0z.B2BSystem.contacts.web.ImportResultResponse;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactService implements ContactInternalApi {

    private final ContactRepository contactRepository;
    private final ContactDetailsViewRepository contactViewRepository;
    private final ContactMapper contactMapper;

    @Transactional(readOnly = true)
    public Page<ContactResponse> getAllContacts(Pageable pageable, ContactFilterRequest filters) {
        log.debug("Querying contacts | filters={}", filters);

        List<Specification<com.faust0z.B2BSystem.contacts.domain.ContactDetailsView>> specs = new ArrayList<>();

        if (filters.isActive() != null) {
            specs.add((root, query, cb) -> {
                if (filters.isActive()) {
                    return cb.isNull(root.get("deletedAt"));
                } else {
                    return cb.isNotNull(root.get("deletedAt"));
                }
            });
        }

        if (filters.searchTerm() != null && !filters.searchTerm().isBlank()) {
            String term = "%" + filters.searchTerm().toLowerCase() + "%";
            specs.add((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("firstName")), term),
                            cb.like(cb.lower(root.get("lastName")), term),
                            cb.like(cb.lower(root.get("dni")), term),
                            cb.like(cb.lower(root.get("currentMembershipName")), term)
                    )
            );
        }

        if (filters.gender() != null) {
            specs.add((root, query, cb) ->
                    cb.equal(root.get("gender"), filters.gender())
            );
        }

        if (filters.hasAccessBegin() != null) {
            specs.add((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("accessUntil"), filters.hasAccessBegin())
            );
        }

        if (filters.hasAccessEnd() != null) {
            specs.add((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("accessUntil"), filters.hasAccessEnd())
            );
        }

        if (filters.lastScannedBefore() != null) {
            specs.add((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("lastScannedAt"), filters.lastScannedBefore())
            );
        }

        if (filters.hasDebt() != null) {
            if (Boolean.TRUE.equals(filters.hasDebt())) {
                specs.add((root, query, cb) ->
                        cb.greaterThan(root.get("debtAmount"), 0)
                );
            } else {
                specs.add((root, query, cb) ->
                        cb.equal(root.get("debtAmount"), 0)
                );
            }
        }

        if (filters.hasEntered() != null) {
            if (Boolean.TRUE.equals(filters.hasEntered())) {
                specs.add((root, query, cb) ->
                        cb.isNotNull(root.get("lastScannedAt"))
                );
            } else {
                specs.add((root, query, cb) ->
                        cb.isNull(root.get("lastScannedAt"))
                );
            }
        }

        return contactViewRepository.findAll(Specification.allOf(specs), pageable)
                .map(com.faust0z.B2BSystem.contacts.domain.ContactDetailsView::toResponse);
    }

    @Cacheable(value = "contact_quick_stats", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId()")
    @Transactional(readOnly = true)
    public ContactQuickStatsResponse getQuickStats() {
        log.debug("Fetching quick stats");

        Instant now = Instant.now();
        Instant expiringCutoff = now.plus(7, ChronoUnit.DAYS);
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);

        var stats = contactViewRepository.getQuickStats(now, expiringCutoff, sevenDaysAgo);

        return new ContactQuickStatsResponse(
                stats.total() != null ? stats.total() : 0L,
                stats.expired() != null ? stats.expired() : 0L,
                stats.withDebt() != null ? stats.withDebt() : 0L,
                stats.notCheckedIn() != null ? stats.notCheckedIn() : 0L
        );
    }

    @Cacheable(value = "contact", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId() + ':' + #contactId")
    @Transactional(readOnly = true)
    public ContactResponse getContactById(UUID contactId) {
        log.debug("Fetching contact | contactId={}", contactId);

        return contactViewRepository.findById(contactId, com.faust0z.B2BSystem.contacts.domain.ContactDetailsView.class)
                .orElseThrow(ContactNotFoundException::new)
                .toResponse();
    }

    @CacheEvict(value = {"contacts", "contact_quick_stats"}, allEntries = true)
    @Transactional
    public ContactResponse createContact(ContactCreateRequest request) {
        log.debug("Creating contact | dni={}", request.dni());

        if (contactRepository.existsByDni(request.dni())) {
            throw new ContactDniAlreadyRegisteredException();
        }

        ContactEntity contactEntity = contactMapper.toEntity(request);
        contactEntity.setTenantId(TenantContext.getTenantId());
        contactEntity = contactRepository.save(contactEntity);

        log.info("Contact created | contactId={}", contactEntity.getId());
        return contactViewRepository.findById(contactEntity.getId(), com.faust0z.B2BSystem.contacts.domain.ContactDetailsView.class)
                .orElseThrow(ContactNotFoundException::new)
                .toResponse();
    }

    @CacheEvict(value = {"contacts", "contact_quick_stats"}, allEntries = true)
    @Transactional
    public ImportResultResponse importContacts(List<ContactImportRequest> requests) {
        log.debug("Importing contacts | count={}", requests.size());

        int imported = 0;
        int skipped = 0;

        for (ContactImportRequest request : requests) {
            try {
                if (contactRepository.existsByDni(request.dni())) {
                    skipped++;
                    continue;
                }

                ContactEntity contactEntity = new ContactEntity();
                contactEntity.setTenantId(TenantContext.getTenantId());
                contactEntity.setFirstName(request.nombre());
                contactEntity.setLastName(request.apellido());
                contactEntity.setDni(request.dni());
                contactEntity.setEmail(request.email());
                contactEntity.setBirthDate(request.birthDate());
                contactEntity.setPhoneNumber(request.phoneNumber());
                contactEntity.setGender(request.gender());

                if (request.accessUntil() != null) {
                    LocalDate accessUntilDate = request.accessUntil();
                    ZonedDateTime zonedDateTime = accessUntilDate.atStartOfDay(ZoneId.of("America/Argentina/Buenos_Aires"));
                    contactEntity.setAccessUntil(zonedDateTime.toInstant());
                }

                contactRepository.save(contactEntity);
                imported++;
            } catch (Exception e) {
                log.warn("Failed to import contact | dni={} | error={}", request.dni(), e.getMessage());
                skipped++;
            }
        }

        log.info("Contact import completed | imported={} | skipped={}", imported, skipped);
        return new ImportResultResponse(imported, skipped);
    }

    @CacheEvict(value = {"contacts", "contact_quick_stats"}, allEntries = true)
    @Transactional
    public ContactResponse updateContact(UUID contactId, ContactUpdateRequest request) {
        log.debug("Updating contact | contactId={}", contactId);

        ContactEntity contactEntity = contactRepository.findById(contactId)
                .orElseThrow(ContactNotFoundException::new);

        contactMapper.updateContactFromRequest(request, contactEntity);

        if (request.accessUntil() != null) {
            ZonedDateTime zonedDateTime = request.accessUntil().atZone(ZoneId.of("America/Argentina/Buenos_Aires"));
            contactEntity.setAccessUntil(zonedDateTime.toInstant());
        }

        contactRepository.save(contactEntity);

        log.debug("Successfully updated contact | contactId={}", contactId);
        return contactViewRepository.findById(contactId, com.faust0z.B2BSystem.contacts.domain.ContactDetailsView.class)
                .orElseThrow(ContactNotFoundException::new)
                .toResponse();
    }

    @CachePut(value = "contact", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId() + ':' + #contactId")
    @Transactional
    public ContactResponse toggleContactStatus(UUID contactId) {
        log.debug("Toggling contact status | contactId={}", contactId);

        ContactEntity contactEntity = contactRepository.findById(contactId)
                .orElseThrow(ContactNotFoundException::new);

        if (contactEntity.getDeletedAt() == null) {
            contactEntity.softDelete();
        } else {
            contactEntity.setDeletedAt(null);
        }
        contactRepository.save(contactEntity);

        log.debug("Contact status toggled | contactId={} | deletedAt={}", contactId, contactEntity.getDeletedAt());
        return contactViewRepository.findById(contactId, com.faust0z.B2BSystem.contacts.domain.ContactDetailsView.class)
                .orElseThrow(ContactNotFoundException::new)
                .toResponse();
    }

    @Override
    public ContactAccessInternalResponse findActiveContact(String identifier, ScanType scanType) {
        log.debug("Finding active contact | identifier={} | scanType={}", identifier, scanType);

        Optional<ContactEntity> contactOpt;
        if (scanType == ScanType.DNI) {
            contactOpt = contactRepository.findByDni(identifier, ContactEntity.class);
        } else {
            contactOpt = contactRepository.findByAccessToken(UUID.fromString(identifier), ContactEntity.class);
        }

        ContactEntity contact = contactOpt.orElseThrow(ContactNotFoundException::new);

        if (contact.getDeletedAt() != null) {
            throw new ContactDisabledException();
        }

        return new ContactAccessInternalResponse(
                contact.getId(),
                contact.getFirstName(),
                contact.getLastName(),
                contact.getCurrentMembershipName(),
                contact.getAccessUntil(),
                contact.getDebtAmount(),
                contact.getDeletedAt() == null
        );
    }

    @Override
    public ContactInternalStats getContactStats(Instant currentTime) {
        log.debug("Getting contact stats | currentTime={}", currentTime);

        var stats = contactRepository.getDashboardStats(currentTime);

        long active = stats.active() != null ? stats.active() : 0L;
        long expired = stats.expired() != null ? stats.expired() : 0L;

        return new ContactInternalStats(stats.total(), active, expired);
    }

    @Override
    public UUID validateContactActive(UUID contactId) {
        log.debug("Validating contact active | contactId={}", contactId);

        ContactEntity contact = contactRepository.findById(contactId)
                .orElseThrow(ContactNotFoundException::new);

        if (contact.getDeletedAt() != null) {
            throw new ContactDisabledException();
        }

        return contactId;
    }

    @Override
    public int countContactsWithAccess(Instant referenceDate) {
        log.debug("Counting contacts with access | referenceDate={}", referenceDate);
        return contactRepository.countContactsWithAccess(referenceDate);
    }

    @Override
    public ContactAccessInternalResponse findActiveContactByDni(String dni) {
        log.debug("Finding active contact by DNI | dni={}", dni);

        ContactEntity contact = contactRepository.findByDni(dni, ContactEntity.class)
                .orElseThrow(ContactNotFoundException::new);

        if (contact.getDeletedAt() != null) {
            throw new ContactDisabledException();
        }

        return new ContactAccessInternalResponse(
                contact.getId(),
                contact.getFirstName(),
                contact.getLastName(),
                contact.getCurrentMembershipName(),
                contact.getAccessUntil(),
                contact.getDebtAmount(),
                contact.getDeletedAt() == null
        );
    }

    @Override
    public Optional<Boolean> getContactStatus(String dni) {
        log.debug("Getting contact status | dni={}", dni);

        return contactRepository.findByDni(dni, ContactEntity.class)
                .map(contact -> {
                    boolean canAccess = contact.getDeletedAt() == null
                            && contact.getAccessUntil() != null
                            && contact.getAccessUntil().isAfter(Instant.now());
                    return canAccess;
                });
    }

    @Transactional
    public void extendMembership(UUID contactId, String membershipName, Integer durationDays, Instant paidAt) {
        log.debug("Extending membership | contactId={} | membershipName={} | durationDays={}", contactId, membershipName, durationDays);

        ContactEntity contact = contactRepository.findById(contactId)
                .orElseThrow(ContactNotFoundException::new);

        if (durationDays != null && paidAt != null) {
            Instant newAccessUntil = paidAt.plus(durationDays, ChronoUnit.DAYS);
            contact.setAccessUntil(newAccessUntil);
        }

        if (membershipName != null) {
            contact.setCurrentMembershipName(membershipName);
        }

        contactRepository.save(contact);
        log.debug("Membership extended | contactId={} | accessUntil={}", contactId, contact.getAccessUntil());
    }

    @Transactional
    public void reduceMembership(UUID contactId, Integer durationDays) {
        log.debug("Reducing membership | contactId={} | durationDays={}", contactId, durationDays);

        ContactEntity contact = contactRepository.findById(contactId)
                .orElseThrow(ContactNotFoundException::new);

        if (durationDays != null && contact.getAccessUntil() != null) {
            Instant newAccessUntil = contact.getAccessUntil().minus(durationDays, ChronoUnit.DAYS);
            contact.setAccessUntil(newAccessUntil);
        }

        contactRepository.save(contact);
        log.debug("Membership reduced | contactId={} | accessUntil={}", contactId, contact.getAccessUntil());
    }
}
