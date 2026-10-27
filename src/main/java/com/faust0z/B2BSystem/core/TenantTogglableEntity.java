package com.faust0z.B2BSystem.core;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class TenantTogglableEntity extends TenantMutableEntity {

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}