package com.faust0z.B2BSystem.user.infrastructure;

import com.faust0z.B2BSystem.user.Role;
import com.faust0z.B2BSystem.user.UserAuthInternalResponse;
import com.faust0z.B2BSystem.user.UserResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AuthLookupRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuthLookupRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<UserAuthInternalResponse> USER_AUTH_ROW_MAPPER = (rs, rowNum) -> {
        String roleStr = rs.getString("role");
        Role role = roleStr != null ? Role.valueOf(roleStr) : Role.TENANT_USER;
        return new UserAuthInternalResponse(
                uuidOrNull(rs, "tenant_id"),
                uuidOrNull(rs, "id"),
                rs.getString("email"),
                rs.getString("password"),
                role,
                rs.getObject("deleted_at", Instant.class) == null
        );
    };

    private static final RowMapper<UserResponse> USER_RESPONSE_ROW_MAPPER = (rs, rowNum) -> {
        String roleStr = rs.getString("role");
        Role role = roleStr != null ? Role.valueOf(roleStr) : Role.TENANT_USER;
        return new UserResponse(
                uuidOrNull(rs, "id"),
                uuidOrNull(rs, "tenant_id"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("username"),
                role,
                rs.getObject("deleted_at", Instant.class) == null
        );
    };

    public Optional<UserAuthInternalResponse> findUserAuthByIdentifier(String identifier) {
        String sql = """
            SELECT id, tenant_id, email, password, role, deleted_at
            FROM users
            WHERE (LOWER(email) = LOWER(?) OR LOWER(username) = LOWER(?))
              AND deleted_at IS NULL
            LIMIT 1
        """;
        return jdbcTemplate.query(sql, USER_AUTH_ROW_MAPPER, identifier, identifier)
                .stream().findFirst();
    }

    public Optional<UserResponse> findUserResponseByIdentifier(String identifier) {
        String sql = """
            SELECT id, tenant_id, full_name, email, username, role, deleted_at
            FROM users
            WHERE (LOWER(email) = LOWER(?) OR LOWER(username) = LOWER(?))
              AND deleted_at IS NULL
            LIMIT 1
        """;
        return jdbcTemplate.query(sql, USER_RESPONSE_ROW_MAPPER, identifier, identifier)
                .stream().findFirst();
    }

    public Optional<UserAuthInternalResponse> findUserById(UUID userId) {
        String sql = """
            SELECT id, tenant_id, email, password, role, deleted_at
            FROM users
            WHERE id = ?
              AND deleted_at IS NULL
            LIMIT 1
        """;
        return jdbcTemplate.query(sql, USER_AUTH_ROW_MAPPER, userId)
                .stream().findFirst();
    }

    public Optional<UserResponse> findUserResponseById(UUID userId) {
        String sql = """
            SELECT id, tenant_id, full_name, email, username, role, deleted_at
            FROM users
            WHERE id = ?
              AND deleted_at IS NULL
            LIMIT 1
        """;
        return jdbcTemplate.query(sql, USER_RESPONSE_ROW_MAPPER, userId)
                .stream().findFirst();
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(?) AND deleted_at IS NULL";
        return jdbcTemplate.queryForObject(sql, Integer.class, email) > 0;
    }

    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE LOWER(username) = LOWER(?) AND deleted_at IS NULL";
        return jdbcTemplate.queryForObject(sql, Integer.class, username) > 0;
    }

    public Optional<UUID> getTenantIdByUserId(UUID userId) {
        String sql = "SELECT tenant_id FROM users WHERE id = ? AND deleted_at IS NULL LIMIT 1";
        return jdbcTemplate.query(sql, (rs, rowNum) -> uuidOrNull(rs, "tenant_id"), userId)
                .stream().findFirst();
    }

    private static UUID uuidOrNull(java.sql.ResultSet rs, String columnName) {
        try {
            return rs.getObject(columnName, UUID.class);
        } catch (Exception e) {
            return null;
        }
    }
}