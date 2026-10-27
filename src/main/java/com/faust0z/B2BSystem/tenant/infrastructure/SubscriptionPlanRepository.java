package com.faust0z.B2BSystem.tenant.infrastructure;

import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanEntity;
import com.faust0z.B2BSystem.tenant.infrastructure.SubscriptionPlanRepository;
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlanEntity, UUID>, JpaSpecificationExecutor<SubscriptionPlanEntity> {

    <T> List<T> findBy(Class<T> type);

    <T> Optional<T> findById(UUID id, Class<T> type);

    List<SubscriptionPlanEntity> findByIsActiveTrue();
}