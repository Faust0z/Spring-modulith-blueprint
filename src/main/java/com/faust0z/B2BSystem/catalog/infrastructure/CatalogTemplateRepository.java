package com.faust0z.B2BSystem.catalog.infrastructure;

import com.faust0z.B2BSystem.catalog.domain.CatalogTemplateEntity;
import com.faust0z.B2BSystem.catalog.CatalogTemplateSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CatalogTemplateRepository extends JpaRepository<CatalogTemplateEntity, UUID>, JpaSpecificationExecutor<CatalogTemplateEntity> {
    @Query("""
            SELECT new com.faust0z.B2BSystem.catalog.CatalogTemplateSummaryResponse(r.id, r.name, r.isActive, r.category)
            FROM CatalogTemplateEntity r
            WHERE r.isActive = true
            ORDER BY r.createdAt DESC
            """)
    List<CatalogTemplateSummaryResponse> findActiveSummaries();

    <T> Page<T> findBy(Pageable pageable, Class<T> type);

    <T> Optional<T> findById(UUID id, Class<T> type);
}
