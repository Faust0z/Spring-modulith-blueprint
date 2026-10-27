package com.faust0z.B2BSystem.contacts.infrastructure;

import com.faust0z.B2BSystem.contacts.domain.ContactEntity;
import com.faust0z.B2BSystem.contacts.domain.ContactStatsProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface ContactRepository extends JpaRepository<ContactEntity, UUID>, JpaSpecificationExecutor<ContactEntity> {
    <T> Optional<T> findById(UUID id, Class<T> type);

    <T> Optional<T> findByDni(String dni, Class<T> type);

    <T> Optional<T> findByAccessToken(UUID accessToken, Class<T> type);

    boolean existsByDni(String dni);

    @Query("""
                SELECT new com.faust0z.B2BSystem.contacts.ContactStatsProjection(
                    COUNT(m),
                    SUM(CASE WHEN m.accessUntil > :now THEN 1L ELSE 0L END),
                    SUM(CASE WHEN m.accessUntil <= :now THEN 1L ELSE 0L END)
                )
                FROM ContactEntity m
            """)
    ContactStatsProjection getDashboardStats(@Param("now") Instant now);

    @Query("SELECT m.dni FROM ContactEntity m WHERE m.dni IN :dnis")
    Set<String> findDnisByDniIn(@Param("dnis") Set<String> dnis);

    @Query("SELECT COUNT(m) FROM ContactEntity m WHERE m.accessUntil >= :referenceDate")
    int countContactsWithAccess(@Param("referenceDate") Instant referenceDate);
}
