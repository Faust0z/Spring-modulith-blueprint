package com.faust0z.B2BSystem.catalog.application;

import com.faust0z.B2BSystem.catalog.ServicePlanInternalApi;
import com.faust0z.B2BSystem.catalog.ServicePlanResponse;
import com.faust0z.B2BSystem.catalog.domain.ServicePlanEntity;
import com.faust0z.B2BSystem.catalog.infrastructure.ServicePlanRepository;
import com.faust0z.B2BSystem.catalog.web.ServicePlanCreateRequest;
import com.faust0z.B2BSystem.catalog.web.ServicePlanNotFoundException;
import com.faust0z.B2BSystem.catalog.web.ServicePlanUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServicePlanService implements ServicePlanInternalApi {

    private final ServicePlanRepository servicePlanRepository;
    private final ServicePlanMapper servicePlanMapper;

    @Override
    @Transactional
    public ServicePlanResponse createDefaultServicePlan() {
        ServicePlanEntity servicePlanEntity = new ServicePlanEntity();
        servicePlanEntity.setName("Plan Mensual de Ejemplo");
        servicePlanEntity.setDurationDays(30);
        servicePlanEntity.setPrice(BigDecimal.valueOf(30000));
        servicePlanEntity = servicePlanRepository.save(servicePlanEntity);

        log.info("Default service plan created | servicePlanId={}", servicePlanEntity.getId());
        return servicePlanMapper.toResponse(servicePlanEntity);
    }

    @Cacheable(value = "tenant_service_plans", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId()")
    @Transactional(readOnly = true)
    public List<ServicePlanResponse> getAllServicePlans() {
        log.debug("Querying service plans");
        return servicePlanRepository.findBy(Sort.by(Sort.Direction.ASC, "price"), ServicePlanResponse.class);
    }

    @CacheEvict(value = "tenant_service_plans", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId()")
    @Transactional
    public ServicePlanResponse createServicePlan(ServicePlanCreateRequest request) {
        log.debug("Creating service plans");

        ServicePlanEntity servicePlanEntity = servicePlanMapper.toEntity(request);
        servicePlanEntity = servicePlanRepository.save(servicePlanEntity);

        log.debug("Service plans created | servicePlanId={}", servicePlanEntity.getId());
        return servicePlanMapper.toResponse(servicePlanEntity);
    }

    @Caching(evict = {
            @CacheEvict(value = "tenant_service_plans", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId()"),
            @CacheEvict(value = "service_plan_internal", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId() + ':' + #servicePlanId")
    })
    @Transactional
    public ServicePlanResponse updateServicePlan(UUID servicePlanId, ServicePlanUpdateRequest request) {
        log.debug("Updating service plan | servicePlanId={}", servicePlanId);

        ServicePlanEntity servicePlanEntity = servicePlanRepository.findById(servicePlanId).orElseThrow(() -> new ServicePlanNotFoundException());

        servicePlanMapper.updateServicePlanFromRequest(request, servicePlanEntity);

        log.debug("Successfully updated service plan | servicePlanId={}", servicePlanId);
        return servicePlanMapper.toResponse(servicePlanEntity);
    }

    @Caching(evict = {
            @CacheEvict(value = "tenant_service_plans", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId()"),
            @CacheEvict(value = "service_plan_internal", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId() + ':' + #servicePlanId")
    })
    @Transactional
    public ServicePlanResponse toggleServicePlanStatus(UUID servicePlanId) {
        log.debug("Updating service plan status | servicePlanId={}", servicePlanId);

        ServicePlanEntity servicePlanEntity = servicePlanRepository.findById(servicePlanId).orElseThrow(() -> new ServicePlanNotFoundException());

        if (servicePlanEntity.getDeletedAt() == null) {
            servicePlanEntity.softDelete();
        } else {
            servicePlanEntity.setDeletedAt(null);
        }

        log.debug("Service plan status updated | servicePlanId={} | deletedAt={}", servicePlanId, servicePlanEntity.getDeletedAt());
        return servicePlanMapper.toResponse(servicePlanRepository.save(servicePlanEntity));
    }

    @Override
    public Optional<ServicePlanResponse> findById(UUID id) {
        return servicePlanRepository.findById(id, ServicePlanResponse.class);
    }
}
