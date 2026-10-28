package com.faust0z.B2BSystem.access.infrastructure;

import com.faust0z.B2BSystem.access.domain.AccessLogSummary;
import com.faust0z.B2BSystem.access.domain.AccessStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class AccessLogDetailsRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<AccessLogSummary> ACCESS_LOG_SUMMARY_ROW_MAPPER = (rs, rowNum) -> new AccessLogSummary(
            uuidOrNull(rs, "id"),
            uuidOrNull(rs, "tenant_id"),
            uuidOrNull(rs, "contact_id"),
            rs.getString("dni"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getString("current_service_plan_name"),
            rs.getObject("access_until", Instant.class),
            rs.getObject("scanned_at", Instant.class),
            AccessStatus.valueOf(rs.getString("status")),
            rs.getObject("contact_debt_amount", Integer.class),
            uuidOrNull(rs, "location_id")
    );

    private static final String BASE_SQL = """
            SELECT al.id, al.tenant_id, al.contact_id, c.dni, c.first_name, c.last_name,
                   c.current_service_plan_name, c.access_until, al.scanned_at, al.status,
                   c.debt_amount AS contact_debt_amount, al.location_id
            FROM access_logs al
            JOIN contacts c ON al.contact_id = c.id AND al.tenant_id = c.tenant_id
            WHERE al.tenant_id = ? AND al.deleted_at IS NULL
            """;

    public AccessLogDetailsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Slice<AccessLogSummary> findAllByTenantId(UUID tenantId, Pageable pageable) {
        int offset = pageable.getPageNumber() * pageable.getPageSize();
        int limit = pageable.getPageSize() + 1;

        String sortClause = buildSortClause(pageable);
        String sql = BASE_SQL + sortClause + " LIMIT ? OFFSET ?";

        List<AccessLogSummary> results = jdbcTemplate.query(
                sql,
                ACCESS_LOG_SUMMARY_ROW_MAPPER,
                tenantId, limit, offset
        );

        boolean hasNext = results.size() > pageable.getPageSize();
        if (hasNext) {
            results = results.subList(0, pageable.getPageSize());
        }

        return new AccessLogSummarySlice(results, pageable, hasNext);
    }

    private static String buildSortClause(Pageable pageable) {
        if (pageable.isUnpaged()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(" ORDER BY ");
        boolean first = true;
        for (var order : pageable.getSort()) {
            if (!first) sb.append(", ");
            sb.append(toSnakeCase(order.getProperty())).append(" ").append(order.getDirection());
            first = false;
        }
        return sb.toString();
    }

    private static String toSnakeCase(String camelCase) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) sb.append('_');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static UUID uuidOrNull(ResultSet rs, String columnName) {
        try {
            return rs.getObject(columnName, UUID.class);
        } catch (Exception e) {
            return null;
        }
    }
}