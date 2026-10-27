package com.faust0z.B2BSystem.tenant.domain;

import com.faust0z.B2BSystem.core.TenantTogglableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tenants")
public class Tenant extends TenantTogglableEntity {
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "tax_id", unique = true)
    private String taxId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", columnDefinition = "jsonb")
    private Map<String, Object> settings = new HashMap<>();

    @Column(name = "slug", unique = true, nullable = false, length = 30)
    private String slug;
}
