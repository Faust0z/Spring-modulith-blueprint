package com.faust0z.B2BSystem.locations;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.faust0z.B2BSystem.locations.Location;
import com.faust0z.B2BSystem.locations.LocationRepository;
@Repository
public interface LocationRepository extends JpaRepository<Location, UUID>, JpaSpecificationExecutor<Location> {
    <T> Optional<T> findById(UUID id, Class<T> type);

    <T> List<T> findAllBy(Class<T> type);

    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT b.id FROM Location b WHERE b.isActive = true")
    Set<UUID> findActiveIds();
}
