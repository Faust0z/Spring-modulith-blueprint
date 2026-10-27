package com.faust0z.B2BSystem.catalog.infrastructure;

import com.faust0z.B2BSystem.catalog.domain.ServicePlanEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServicePlanRepository extends JpaRepository<ServicePlanEntity, UUID>, JpaSpecificationExecutor<ServicePlanEntity> {
    <T> List<T> findBy(Sort sort, Class<T> type);

    <T> Optional<T> findById(UUID id, Class<T> type);
}
