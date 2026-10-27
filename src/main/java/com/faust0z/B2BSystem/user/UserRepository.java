package com.faust0z.B2BSystem.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.faust0z.B2BSystem.user.User;
import com.faust0z.B2BSystem.user.UserRepository;
@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByIdAndTenantIdAndAdminPinNotNull(UUID userId, UUID tenantId);

    <T> Optional<T> findByEmailIgnoreCaseOrUsernameIgnoreCase(String email, String username, Class<T> type);

    <T> Optional<T> findById(UUID id, Class<T> type);

    <T> Optional<T> findByIdAndTenantId(UUID userId, UUID tenantId, Class<T> type);

    <T> List<T> findAllByTenantId(UUID tenantId, Class<T> type);
}
