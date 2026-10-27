package com.faust0z.B2BSystem.tenant.infrastructure;

import com.faust0z.B2BSystem.tenant.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.faust0z.B2BSystem.tenant.domain.Tenant;
import com.faust0z.B2BSystem.tenant.infrastructure.TenantRepository;
@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID>, JpaSpecificationExecutor<Tenant> {
    boolean existsByTaxId(String taxId);

    <T> Optional<T> findById(UUID id, Class<T> type);

    <T> List<T> findAllBy(Class<T> type);

    boolean existsBySlug(String slug);

    <T> Optional<T> findBySlug(String slug, Class<T> type);
}
