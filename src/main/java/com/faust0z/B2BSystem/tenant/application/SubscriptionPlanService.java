package com.faust0z.B2BSystem.tenant.application;

import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanEntity;
import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanNotFoundException;
import com.faust0z.B2BSystem.tenant.infrastructure.SubscriptionPlanRepository;
import com.faust0z.B2BSystem.tenant.web.SubscriptionPlanCreateRequest;
import com.faust0z.B2BSystem.tenant.web.SubscriptionPlanResponse;
import com.faust0z.B2BSystem.tenant.web.SubscriptionPlanUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import com.faust0z.B2BSystem.tenant.application.SubscriptionPlanMapper;
import com.faust0z.B2BSystem.tenant.application.SubscriptionPlanService;
import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanEntity;
import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanNotFoundException;
import com.faust0z.B2BSystem.tenant.infrastructure.SubscriptionPlanRepository;
import com.faust0z.B2BSystem.tenant.web.SubscriptionPlanCreateRequest;
import com.faust0z.B2BSystem.tenant.web.SubscriptionPlanResponse;
import com.faust0z.B2BSystem.tenant.web.SubscriptionPlanUpdateRequest;
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanMapper subscriptionPlanMapper;

    @Cacheable(value = "subscription_plans", key = "T(com.faust0z.B2BSystem.core.TenantContext).getTenantId()")
    public List<SubscriptionPlanResponse> findAll() {
        log.debug("Querying subscription plans");
        return subscriptionPlanRepository.findBy(SubscriptionPlanResponse.class);
    }

    public SubscriptionPlanResponse findById(UUID id) {
        log.debug("Querying subscription plan | subscriptionPlanId={}", id);
        return subscriptionPlanRepository.findById(id, SubscriptionPlanResponse.class).orElseThrow(SubscriptionPlanNotFoundException::new);
    }

    @CacheEvict(value = "subscription_plans")
    @Transactional
    public SubscriptionPlanResponse create(SubscriptionPlanCreateRequest request) {
        log.debug("Creating subscription plan");

        SubscriptionPlanEntity planEntity = subscriptionPlanMapper.toEntity(request);
        planEntity = subscriptionPlanRepository.save(planEntity);

        log.info("Subscription plan created | subscriptionPlanId={}", planEntity.getId());
        return subscriptionPlanMapper.toResponse(planEntity);
    }

    @CacheEvict(value = "subscription_plans")
    @Transactional
    public SubscriptionPlanResponse update(UUID id, SubscriptionPlanUpdateRequest request) {
        log.debug("Updating subscription plan | subscriptionPlanId={}", id);

        SubscriptionPlanEntity planEntity = subscriptionPlanRepository.findById(id).orElseThrow(SubscriptionPlanNotFoundException::new);
        subscriptionPlanMapper.updateFromRequest(request, planEntity);

        log.info("Subscription plan updated | subscriptionPlanId={}", planEntity.getId());
        return subscriptionPlanMapper.toResponse(planEntity);
    }
}
