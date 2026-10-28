package com.faust0z.B2BSystem.catalog.domain;

import com.faust0z.B2BSystem.core.TenantSoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "catalog_templates")
@SQLRestriction("deleted_at IS NULL")
public class CatalogTemplateEntity extends TenantSoftDeletableEntity {

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "content", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private TemplateCategory category;
}
