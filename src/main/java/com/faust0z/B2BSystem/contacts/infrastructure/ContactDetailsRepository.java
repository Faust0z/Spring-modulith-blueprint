package com.faust0z.B2BSystem.contacts.infrastructure;

import com.faust0z.B2BSystem.contacts.domain.ContactSummary;
import com.faust0z.B2BSystem.contacts.domain.GenderCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public class ContactDetailsRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<ContactSummary> CONTACT_SUMMARY_ROW_MAPPER = (rs, rowNum) -> new ContactSummary(
            uuidOrNull(rs, "id"),
            uuidOrNull(rs, "tenant_id"),
            rs.getString("dni"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getObject("access_until", Instant.class),
            rs.getString("current_membership_name"),
            uuidOrNull(rs, "qr_token"),
            rs.getObject("created_at", Instant.class),
            rs.getString("email"),
            rs.getObject("birth_date", LocalDate.class),
            rs.getString("phone_number"),
            GenderCategory.valueOf(rs.getString("gender")),
            rs.getInt("debt_amount")
    );

    private static final String BASE_SQL = """
            SELECT id, tenant_id, dni, first_name, last_name, access_until,
                   current_membership_name, qr_token, created_at, email,
                   birth_date, phone_number, gender, debt_amount
            FROM members
            WHERE tenant_id = ? AND deleted_at IS NULL
            """;

    public ContactDetailsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Slice<ContactSummary> findAllByTenantId(UUID tenantId, Pageable pageable) {
        int offset = pageable.getPageNumber() * pageable.getPageSize();
        int limit = pageable.getPageSize() + 1;

        String sortClause = buildSortClause(pageable);
        String sql = BASE_SQL + sortClause + " LIMIT ? OFFSET ?";

        List<ContactSummary> results = jdbcTemplate.query(
                sql,
                CONTACT_SUMMARY_ROW_MAPPER,
                tenantId, limit, offset
        );

        boolean hasNext = results.size() > pageable.getPageSize();
        if (hasNext) {
            results = results.subList(0, pageable.getPageSize());
        }

        return new ContactSummarySlice(results, pageable, hasNext);
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