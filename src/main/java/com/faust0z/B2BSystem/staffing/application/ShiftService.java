package com.faust0z.B2BSystem.staffing.application;

import com.faust0z.B2BSystem.core.DomainRuleException;
import com.faust0z.B2BSystem.core.EndDateBeforeStartException;
import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.core.TooManyRequestsException;
import com.faust0z.B2BSystem.locations.LocationInternalApi;
import com.faust0z.B2BSystem.staffing.ShiftInternalApi;
import com.faust0z.B2BSystem.staffing.application.StaffService;
import com.faust0z.B2BSystem.staffing.domain.ShiftEntity;
import com.faust0z.B2BSystem.staffing.infrastructure.ShiftDetailsViewRepository;
import com.faust0z.B2BSystem.staffing.infrastructure.ShiftRepository;
import com.faust0z.B2BSystem.staffing.infrastructure.StaffRepository;
import com.faust0z.B2BSystem.staffing.web.DaysRangeTooWideException;
import com.faust0z.B2BSystem.staffing.web.ShiftFilterRequest;
import com.faust0z.B2BSystem.staffing.web.ShiftProcessRequest;
import com.faust0z.B2BSystem.staffing.web.ShiftResponse;
import com.faust0z.B2BSystem.staffing.web.StaffResponse;
import com.faust0z.B2BSystem.staffing.web.ShiftNotFoundException;
import com.faust0z.B2BSystem.staffing.domain.ShiftsHoursProjection;
import com.faust0z.B2BSystem.staffing.domain.ShiftsHoursResponse;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftService implements ShiftInternalApi {

    private final ShiftRepository shiftRepository;
    private final ShiftDetailsViewRepository shiftDetailsViewRepository;
    private final LocationInternalApi locationInternalApi;
    private final StaffRepository staffRepository;
    private final StaffService staffService;

    @Transactional(readOnly = true)
    public Page<ShiftResponse> getAllShifts(Pageable pageable, ShiftFilterRequest filters) {
        log.debug("Fetching shifts | filters={}", filters);

        List<Specification<com.faust0z.B2BSystem.staffing.infrastructure.ShiftDetailsView>> specs = new ArrayList<>();

        if (filters.searchTerm() != null && !filters.searchTerm().isBlank()) {
            String[] tokens = filters.searchTerm().toLowerCase().split("\\s+");
            specs.add((root, query, cb) -> {
                Predicate[] tokenPredicates = Arrays.stream(tokens).map(token -> {
                    String prefixPattern = token + "%";
                    return cb.or(
                            cb.like(cb.lower(root.get("firstName")), prefixPattern),
                            cb.like(cb.lower(root.get("lastName")), prefixPattern),
                            cb.like(cb.lower(root.get("dni")), prefixPattern)
                    );
                }).toArray(Predicate[]::new);

                return cb.and(tokenPredicates);
            });
        }

        if (filters.clockInAt() != null) {
            specs.add((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("clockInAt"), filters.clockInAt()));
        }

        if (filters.clockOutAt() != null) {
            specs.add((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("clockOutAt"), filters.clockOutAt()));
        }

        if (filters.autoClosed() != null) {
            specs.add((root, query, cb) ->
                    cb.equal(root.get("autoClosed"), filters.autoClosed()));
        }

        if (filters.locationId() != null) {
            specs.add((root, query, cb) ->
                    cb.equal(root.get("locationId"), filters.locationId()));
        }

        if (filters.isOpen() != null) {
            specs.add((root, query, cb) ->
                    filters.isOpen() ? cb.isNull(root.get("clockOutAt")) : cb.isNotNull(root.get("clockOutAt")));
        }

        return shiftDetailsViewRepository.findAll(Specification.allOf(specs), pageable).map(com.faust0z.B2BSystem.staffing.infrastructure.ShiftDetailsView::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ShiftsHoursResponse> getShiftsHoursSummary(LocalDate start, LocalDate end) {
        log.debug("Fetching staff hours in range | start={} | end={}", start, end);

        if (end.isBefore(start)) throw new EndDateBeforeStartException();
        if (ChronoUnit.DAYS.between(start, end) >= 182) throw new DaysRangeTooWideException(182);

        UUID tenantId = TenantContext.getTenantId();
        ZoneId zoneId = ZoneId.of("America/Argentina/Buenos_Aires");
        Instant from = start.atStartOfDay(zoneId).toInstant();
        Instant to = end.atTime(LocalTime.MAX).atZone(zoneId).toInstant();

        List<ShiftsHoursProjection> hoursData = shiftRepository.findHoursGroupedByStaff(from, to, tenantId);

        Set<UUID> staffIds = hoursData.stream().map(ShiftsHoursProjection::getStaffId).collect(Collectors.toSet());
        Map<UUID, StaffResponse> staffMap = staffIds.isEmpty() ? Map.of() :
                staffRepository.findByIdIn(staffIds, StaffResponse.class).stream().collect(Collectors.toMap(StaffResponse::id, u -> u));

        return hoursData.stream()
                .map(data -> {
                    StaffResponse staff = staffMap.get(data.getStaffId());
                    return new ShiftsHoursResponse(
                            staff != null ? staff.firstName() + " " + staff.lastName() : "Unknown staff",
                            staff != null ? staff.dni() : "Unknown dni",
                            data.getTotalHours()
                    );
                }).toList();
    }

    @Override
    @Transactional
    public ShiftResponse processShift(ShiftProcessRequest request) {
        log.debug("Processing shift | identifier={} | type={}", request.identifier(), request.type());

        StaffResponse staffResponse = staffService.getValidStaff(request);
        String fullName = staffResponse.firstName() + " " + staffResponse.lastName();

        return shiftRepository.findByStaffIdAndClockOutAtIsNullAndIsActiveTrue(staffResponse.id())
                .map(shift -> {
                    if (Duration.between(shift.getClockInAt(), Instant.now()).getSeconds() < 10) {
                        throw new TooManyRequestsException(10, "seconds");
                    }
                    shift.setClockOutAt(Instant.now());

                    log.info("Shift closed | shiftId = {} | staffId={}", shift.getId(), shift.getStaffId());
                    return new ShiftResponse(shift.getId(), staffResponse.id(), fullName, shift.getClockInAt(), shift.getClockOutAt(),
                            shift.isAutoClosed(), shift.getClockOutAt() == null, shift.getLocationId());
                })
                .orElseGet(() -> {
                    locationInternalApi.validateLocationActive(request.locationId());

                    ShiftEntity newShift = new ShiftEntity();
                    newShift.setStaffId(staffResponse.id());
                    newShift.setLocationId(request.locationId());
                    newShift.setClockInAt(Instant.now());

                    newShift = shiftRepository.save(newShift);

                    log.info("Shift created | shiftId = {} | staffId={}", newShift.getId(), newShift.getStaffId());
                    return new ShiftResponse(newShift.getId(), staffResponse.id(), fullName, newShift.getClockInAt(), null,
                            false, true, newShift.getLocationId());
                });
    }

    @Transactional
    public void voidShift(UUID shiftId) {
        log.debug("Voiding shift | id={}", shiftId);

        ShiftEntity entity = shiftRepository.findById(shiftId).orElseThrow(ShiftNotFoundException::new);
        if (entity.getClockOutAt() != null) {
            throw new DomainRuleException("This shift is already closed", "Error voiding shift", HttpStatus.BAD_REQUEST);
        }

        entity.setClockOutAt(Instant.now());
        entity.setAutoClosed(true);
        log.info("Shift voided | id={}", shiftId);
    }
}
