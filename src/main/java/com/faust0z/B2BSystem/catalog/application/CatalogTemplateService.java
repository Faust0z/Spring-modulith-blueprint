package com.faust0z.B2BSystem.catalog.application;

import com.faust0z.B2BSystem.catalog.CatalogTemplateInternalApi;
import com.faust0z.B2BSystem.catalog.domain.CatalogTemplateEntity;
import com.faust0z.B2BSystem.catalog.domain.TemplateCategory;
import com.faust0z.B2BSystem.catalog.infrastructure.CatalogTemplateRepository;
import com.faust0z.B2BSystem.catalog.web.CatalogTemplateCreateRequest;
import com.faust0z.B2BSystem.catalog.web.CatalogTemplateDisabledException;
import com.faust0z.B2BSystem.catalog.web.CatalogTemplateFilterRequest;
import com.faust0z.B2BSystem.catalog.web.CatalogTemplateNotFoundException;
import com.faust0z.B2BSystem.catalog.CatalogTemplateResponse;
import com.faust0z.B2BSystem.catalog.CatalogTemplateSummaryResponse;
import com.faust0z.B2BSystem.catalog.web.CatalogTemplateUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogTemplateService implements CatalogTemplateInternalApi {

    private final CatalogTemplateRepository catalogTemplateRepository;
    private final CatalogTemplateMapper catalogTemplateMapper;

    @Transactional(readOnly = true)
    public Page<CatalogTemplateSummaryResponse> getAllRoutines(CatalogTemplateFilterRequest filter, Pageable pageable) {
        log.debug("Querying routines | filter={}", filter);

        List<Specification<CatalogTemplateEntity>> specs = new ArrayList<>();

        if (filter.name() != null && !filter.name().isBlank()) {
            specs.add((root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + filter.name().toLowerCase() + "%"));
        }

        if (filter.category() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("category"), filter.category()));
        }

        return catalogTemplateRepository.findBy(Specification.allOf(specs),
                fluent -> fluent
                        .as(CatalogTemplateSummaryResponse.class)
                        .sortBy(Sort.by(Sort.Direction.DESC, "createdAt"))
                        .page(pageable));
    }

    @Transactional(readOnly = true)
    public CatalogTemplateResponse getRoutineById(UUID routineId) {
        log.debug("Querying routine | routineId={}", routineId);

        return catalogTemplateRepository.findById(routineId, CatalogTemplateResponse.class)
                .orElseThrow(CatalogTemplateNotFoundException::new);
    }

    @Transactional
    public CatalogTemplateResponse createRoutine(CatalogTemplateCreateRequest request) {
        log.debug("Creating routine");

        CatalogTemplateEntity routineEntity = catalogTemplateMapper.toEntity(request);
        routineEntity = catalogTemplateRepository.save(routineEntity);

        log.debug("Routine created | routineId={}", routineEntity.getId());
        return catalogTemplateMapper.toResponse(routineEntity);
    }

    @Transactional
    public CatalogTemplateResponse updateRoutine(UUID routineId, CatalogTemplateUpdateRequest request) {
        log.debug("Updating routine | routineId={}", routineId);

        CatalogTemplateEntity routineEntity = catalogTemplateRepository.findById(routineId).orElseThrow(CatalogTemplateNotFoundException::new);
        catalogTemplateMapper.updateRoutineFromRequest(request, routineEntity);

        log.debug("Successfully updated routine | routineId={}", routineId);
        return catalogTemplateMapper.toResponse(routineEntity);
    }

    @Transactional
    public CatalogTemplateResponse toggleRoutineStatus(UUID routineId) {
        log.debug("Updating routine status | routineId={}", routineId);

        CatalogTemplateEntity routineEntity = catalogTemplateRepository.findById(routineId).orElseThrow(CatalogTemplateNotFoundException::new);

        if (routineEntity.getDeletedAt() == null) {
            routineEntity.softDelete();
        } else {
            routineEntity.setDeletedAt(null);
        }

        log.info("Routine status updated | routineId={} | deletedAt={}", routineId, routineEntity.getDeletedAt());
        return catalogTemplateMapper.toResponse(routineEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogTemplateSummaryResponse> findActiveSummaries() {
        return catalogTemplateRepository.findActiveSummaries();
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogTemplateResponse findById(UUID routineId) {
        CatalogTemplateResponse response = catalogTemplateRepository.findById(routineId, CatalogTemplateResponse.class).orElseThrow(CatalogTemplateNotFoundException::new);
        if (!response.isActive()) throw new CatalogTemplateDisabledException();
        return response;
    }
}
