package com.faust0z.B2BSystem.access.application;

import com.faust0z.B2BSystem.core.EndDateBeforeStartException;
import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.staffing.web.DaysRangeTooWideException;
import com.faust0z.B2BSystem.staffing.web.IncorrectMinutesAmountException;
import com.faust0z.B2BSystem.contacts.domain.ContactAccessInternalResponse;
import com.faust0z.B2BSystem.contacts.ContactInternalApi;
import com.faust0z.B2BSystem.locations.LocationInternalApi;
import com.faust0z.B2BSystem.locations.LocationResponse;
import com.faust0z.B2BSystem.access.web.AccessLogCreateRequest;
import com.faust0z.B2BSystem.access.web.AccessLogResponse;
import com.faust0z.B2BSystem.access.web.AccessLogOccupancyResponse;
import com.faust0z.B2BSystem.access.infrastructure.AccessLogRepository;
import com.faust0z.B2BSystem.access.infrastructure.AccessLogDetailsViewRepository;
import com.faust0z.B2BSystem.access.AccessLogInternalApi;
import com.faust0z.B2BSystem.access.domain.AccessLogEntity;
import com.faust0z.B2BSystem.access.domain.AccessStatus;
import com.faust0z.B2BSystem.access.domain.AccessStatsProjection;
import com.faust0z.B2BSystem.access.domain.DailyCountProjection;
import com.faust0z.B2BSystem.access.domain.HourlyCountProjection;
import com.faust0z.B2BSystem.access.domain.OccupancyProjection;
import com.faust0z.B2BSystem.access.domain.OccupancyDetail;
import com.faust0z.B2BSystem.access.domain.AccessInternalStats;
import com.faust0z.B2BSystem.access.web.AccessLogFilterRequest;
import com.faust0z.B2BSystem.access.web.AccessLogVoidRequest;
import com.faust0z.B2BSystem.access.infrastructure.AccessLogDetailsView;
import com.faust0z.B2BSystem.access.domain.AccessLogNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessLogService implements AccessLogInternalApi {

    private final AccessLogRepository accessLogRepository;
    private final AccessLogDetailsViewRepository accessLogViewRepository;
    private final ContactInternalApi memberInternalApi;
    private final LocationInternalApi branchInternalApi;

    @Transactional(readOnly = true)
    public Page<AccessLogResponse> getAllAccessLogs(Pageable pageable, AccessLogFilterRequest filters) {
        log.debug("Fetching access logs | filters={}", filters);

        List<Specification<AccessLogDetailsView>> specs = new ArrayList<>();

        if (filters.searchTerm() != null && !filters.searchTerm().isBlank()) {
            String[] tokens = filters.searchTerm().toLowerCase().split("\\s+");
            specs.add((root, query, cb) -> {
                Predicate[] tokenPredicates = Arrays.stream(tokens).map(token -> {
                    String prefixPattern = token + "%";
                    return cb.or(
                            cb.like(cb.lower(root.get("firstName")), prefixPattern),
                            cb.like(cb.lower(root.get("lastName")), prefixPattern),
                            cb.like(cb.lower(root.get("dni")), prefixPattern),
                            cb.like(cb.lower(root.get("status")), prefixPattern),
                            cb.like(cb.lower(root.get("current_membership_name")), prefixPattern)
                    );
                }).toArray(Predicate[]::new);

                return cb.and(tokenPredicates);
            });
        }

        if (filters.scannedAtBegin() != null) {
            specs.add((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("scannedAt"), filters.scannedAtBegin()));
        }

        if (filters.scannedAtEnd() != null) {
            specs.add((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("scannedAt"), filters.scannedAtEnd()));
        }

        if (filters.locationId() != null) {
            specs.add((root, query, cb) ->
                    cb.equal(root.get("locationId"), filters.locationId()));
        }

        return accessLogViewRepository.findAll(Specification.allOf(specs), pageable).map(AccessLogDetailsView::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Map<LocalDate, Long>> getDailyAccessStats(LocalDate start, LocalDate end) {
        log.debug("Fetching daily entries stats for all locations | start={} | end={}", start, end);

        Set<UUID> locationIds = branchInternalApi.getActiveLocationIds();

        if (locationIds.isEmpty()) return Map.of();
        if (end.isBefore(start)) throw new EndDateBeforeStartException();
        if (ChronoUnit.DAYS.between(start, end) >= 31) throw new DaysRangeTooWideException(31);

        String timeZoneStr = "America/Argentina/Buenos_Aires";
        ZoneId tz = ZoneId.of(timeZoneStr);
        Instant from = start.atStartOfDay(tz).toInstant();
        Instant to = end.plusDays(1).atStartOfDay(tz).toInstant();

        List<DailyCountProjection> results = accessLogRepository.findDailyStats(TenantContext.getTenantId(), locationIds, from, to, timeZoneStr);

        List<LocalDate> allDates = start.datesUntil(end.plusDays(1)).toList();
        Map<UUID, Map<LocalDate, Long>> targetMap = new LinkedHashMap<>();

        for (UUID locationId : locationIds) {
            Map<LocalDate, Long> profile = new LinkedHashMap<>(32);
            allDates.forEach(date -> profile.put(date, 0L));
            targetMap.put(locationId, profile);
        }

        results.forEach(r -> {
            Map<LocalDate, Long> profile = targetMap.get(r.getLocationId());
            if (profile != null) profile.put(r.getEntryDate(), r.getTotal());
        });

        return targetMap;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Map<Integer, Long>> getHourlyAccessStats(LocalDate start, LocalDate end) {
        log.debug("Fetching hourly access stats for all locations | start={} | end={}", start, end);

        Set<UUID> locationIds = branchInternalApi.getActiveLocationIds();

        if (locationIds.isEmpty()) return Map.of();
        if (end.isBefore(start)) throw new EndDateBeforeStartException();
        if (ChronoUnit.DAYS.between(start, end) >= 31) throw new DaysRangeTooWideException(31);

        String timeZoneStr = "America/Argentina/Buenos_Aires";
        ZoneId tz = ZoneId.of(timeZoneStr);
        Instant from = start.atStartOfDay(tz).toInstant();
        Instant to = end.plusDays(1).atStartOfDay(tz).toInstant();

        List<HourlyCountProjection> results = accessLogRepository.findHourlyStats(TenantContext.getTenantId(), locationIds, from, to, timeZoneStr);

        Map<UUID, Map<Integer, Long>> targetMap = new LinkedHashMap<>();
        for (UUID locationId : locationIds) {
            Map<Integer, Long> profile = new LinkedHashMap<>(24);
            IntStream.range(0, 24).forEach(h -> profile.put(h, 0L));
            targetMap.put(locationId, profile);
        }

        results.forEach(r -> {
            Map<Integer, Long> profile = targetMap.get(r.locationId());
            if (profile != null) profile.put(r.hour(), r.total());
        });

        return targetMap;
    }

    @Override
    @Transactional(readOnly = true)
    public AccessLogOccupancyResponse getCurrentOccupancy(int minutes) {
        log.debug("Fetching current occupancy per location | minutes={}", minutes);

        if (minutes <= 0 || minutes > 1440) throw new IncorrectMinutesAmountException();

        List<LocationResponse> branches = branchInternalApi.getLocations();
        if (branches.isEmpty()) return new AccessLogOccupancyResponse(List.of());

        Set<UUID> activeLocationIds = branches.stream()
                .filter(LocationResponse::isActive)
                .map(LocationResponse::id)
                .collect(Collectors.toSet());

        if (activeLocationIds.isEmpty()) return new AccessLogOccupancyResponse(List.of());

        Instant from = Instant.now().minus(minutes, ChronoUnit.MINUTES);
        Map<UUID, Long> statsMap = accessLogRepository.getCurrentOccupationEstimate(activeLocationIds, AccessStatus.PERMITIDO, from).stream()
                .collect(Collectors.toMap(OccupancyProjection::locationId, OccupancyProjection::total));

        List<OccupancyDetail> locationOccupancies = branches.stream()
                .filter(LocationResponse::isActive)
                .map(b -> new OccupancyDetail(
                        b.id(),
                        b.name(),
                        statsMap.getOrDefault(b.id(), 0L).intValue()
                ))
                .toList();

        return new AccessLogOccupancyResponse(locationOccupancies);
    }

    @Override
    @Transactional
    public AccessLogResponse createAccessLog(AccessLogCreateRequest request) {
        log.debug("Processing access scan | type={} | identifier={} | locationId={}", request.scanType(), request.contactIdentifier(), request.locationId());

        branchInternalApi.validateLocationActive(request.locationId());

        ContactAccessInternalResponse contact = memberInternalApi.findActiveContact(request.contactIdentifier(), request.scanType());

        AccessLogEntity accessLogEntity = new AccessLogEntity();
        accessLogEntity.setContactId(contact.id());
        accessLogEntity.setStatus(determineStatus(contact));
        accessLogEntity.setScannedAt(Instant.now());
        accessLogEntity.setLocationId(request.locationId());

        accessLogEntity = accessLogRepository.save(accessLogEntity);

        return new AccessLogResponse(
                accessLogEntity.getId(),
                accessLogEntity.getContactId(),
                contact.firstName() + " " + contact.lastName(),
                contact.accessUntil(),
                accessLogEntity.getScannedAt(),
                accessLogEntity.getStatus(),
                contact.debtAmount(),
                accessLogEntity.getLocationId()
        );
    }

    @Transactional
    public AccessLogResponse voidEntryLog(UUID accessLogId, AccessLogVoidRequest request) {
        log.debug("Voiding access log | id={} | reason={}", accessLogId, request.reason());

        AccessLogEntity entity = accessLogRepository.findById(accessLogId).orElseThrow(() -> new AccessLogNotFoundException(accessLogId));

        entity.setReason(request.reason());
        entity.setStatus(AccessStatus.ANULADO);

        log.info("Access log voided | id={}", accessLogId);
        return accessLogViewRepository.findById(accessLogId)
                .map(AccessLogDetailsView::toResponse)
                .orElseThrow(() -> new AccessLogNotFoundException(accessLogId));
    }

    private AccessStatus determineStatus(ContactAccessInternalResponse member) {
        if (member.accessUntil() == null) {
            log.debug("Access denied: No membership | contactId={}", member.id());
            return AccessStatus.DENEGADO_IMPAGO;
        }

        if (member.accessUntil().isBefore(Instant.now())) {
            log.debug("Access denied: Membership expired | contactId={} | expiredAt={}", member.id(), member.accessUntil());
            return AccessStatus.DENEGADO_VENCIDO;
        }

        return AccessStatus.PERMITIDO;
    }

    @Override
    public Map<UUID, AccessInternalStats> getAccessStats(Set<UUID> locationIds, Instant startOfDay, Instant startOfWeek, Instant startOfMonth, Instant endOfMonth) {
        log.debug("Fetching member entries statistics for locations");

        if (locationIds.isEmpty()) return Map.of();

        Map<UUID, AccessInternalStats> map = accessLogRepository.getDashboardStats(locationIds, startOfDay, startOfWeek, startOfMonth, endOfMonth).stream()
                .collect(Collectors.toMap(AccessStatsProjection::locationId, p ->
                        new AccessInternalStats(p.today(), p.thisWeek(), p.thisMonth())));

        locationIds.forEach(id -> map.putIfAbsent(id, new AccessInternalStats(0L, 0L, 0L)));
        return map;
    }
}
