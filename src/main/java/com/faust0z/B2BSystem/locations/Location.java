package com.faust0z.B2BSystem.locations;

import com.faust0z.B2BSystem.core.TenantSoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "branches")
public class Location extends TenantSoftDeletableEntity {
    @Column(name = "name", nullable = false, length = 50)
    private String name;
}
