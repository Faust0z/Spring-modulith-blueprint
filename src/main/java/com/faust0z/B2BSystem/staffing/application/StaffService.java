package com.faust0z.B2BSystem.staffing.application;

import com.faust0z.B2BSystem.staffing.StaffInternalApi;
import com.faust0z.B2BSystem.staffing.domain.StaffEntity;
import com.faust0z.B2BSystem.staffing.domain.StaffIdentifierType;
import com.faust0z.B2BSystem.staffing.infrastructure.StaffRepository;
import com.faust0z.B2BSystem.staffing.web.StaffCreateRequest;
import com.faust0z.B2BSystem.staffing.web.StaffDisabledException;
import com.faust0z.B2BSystem.staffing.web.StaffNotFoundException;
import com.faust0z.B2BSystem.staffing.web.StaffResponse;
import com.faust0z.B2BSystem.staffing.web.StaffUpdateRequest;
import com.faust0z.B2BSystem.staffing.web.ShiftProcessRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffService implements StaffInternalApi {

    private final StaffRepository staffRepository;
    private final StaffMapper staffMapper;

    @Cacheable(value = "tenant_staff", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId()")
    @Transactional(readOnly = true)
    public List<StaffResponse> getStaff() {
        log.debug("Querying staff");
        return staffRepository.findBy(StaffResponse.class);
    }

    public StaffResponse getValidStaff(ShiftProcessRequest request) {
        StaffResponse staff;
        if (request.type() == StaffIdentifierType.DNI) {
            staff = staffRepository.findByDni(request.identifier(), StaffResponse.class)
                    .orElseThrow(StaffNotFoundException::new);
        } else if (request.type() == StaffIdentifierType.UUID) {
            staff = staffRepository.findById(UUID.fromString(request.identifier()), StaffResponse.class)
                    .orElseThrow(StaffNotFoundException::new);
        } else {
            throw new StaffNotFoundException();
        }

        if (staff.deletedAt() != null) throw new StaffDisabledException();
        return staff;
    }

    @CacheEvict(value = "tenant_staff", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId()")
    @Transactional
    public StaffResponse createStaff(StaffCreateRequest request) {
        log.debug("Creating staff");

        StaffEntity staffEntity = staffMapper.toEntity(request);
        staffEntity = staffRepository.save(staffEntity);

        log.debug("Staff created | staffId={}", staffEntity.getId());
        return staffMapper.toResponse(staffEntity);
    }

    @Caching(evict = {
            @CacheEvict(value = "tenant_staff", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId()"),
            @CacheEvict(value = "staff", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId() + ':' + #staffId")
    })
    @Transactional
    public StaffResponse updateStaff(UUID staffId, StaffUpdateRequest request) {
        log.debug("Updating staff | staffId={}", staffId);

        StaffEntity staffEntity = staffRepository.findById(staffId).orElseThrow(StaffNotFoundException::new);
        staffMapper.updateFromRequest(request, staffEntity);

        log.debug("Successfully updated staff | staffId={}", staffId);
        return staffMapper.toResponse(staffEntity);
    }

    @Caching(evict = {
            @CacheEvict(value = "tenant_staff", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId()"),
            @CacheEvict(value = "staff", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId() + ':' + #staffId")
    })
    @Transactional
    public StaffResponse toggleStaffStatus(UUID staffId) {
        log.debug("Updating staff status | staffId={}", staffId);

        StaffEntity staffEntity = staffRepository.findById(staffId).orElseThrow(StaffNotFoundException::new);

        if (staffEntity.getDeletedAt() == null) {
            staffEntity.softDelete();
        } else {
            staffEntity.setDeletedAt(null);
        }

        log.debug("Staff status updated | staffId={} | deletedAt={}", staffId, staffEntity.getDeletedAt());
        return staffMapper.toResponse(staffEntity);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "staff_identification", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId() + ':' + #dni", unless = "#result == null || !#result")
    public Optional<Boolean> getStaffStatus(String dni) {
        return staffRepository.findByDni(dni, StaffEntity.class).map(e -> e.getDeletedAt() == null);
    }
}
