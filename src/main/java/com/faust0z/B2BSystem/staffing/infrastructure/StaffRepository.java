package com.faust0z.B2BSystem.staffing.infrastructure;

import com.faust0z.B2BSystem.staffing.domain.StaffEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface StaffRepository extends JpaRepository<StaffEntity, UUID>, JpaSpecificationExecutor<StaffEntity> {
    <T> List<T> findBy(Class<T> type);

    <T> List<T> findByIdIn(Set<UUID> ids, Class<T> type);

    <T> Optional<T> findById(UUID id, Class<T> type);

    <T> Optional<T> findByDni(String dni, Class<T> type);
}
