package com.faust0z.B2BSystem.access.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface AccessLogDetailsViewRepository extends JpaRepository<AccessLogDetailsView, UUID>, JpaSpecificationExecutor<AccessLogDetailsView> {
    <T> Optional<T> findById(UUID id, Class<T> type);
}
