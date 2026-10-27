package com.faust0z.B2BSystem.staffing.infrastructure;

import com.faust0z.B2BSystem.staffing.infrastructure.ShiftDetailsView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ShiftDetailsViewRepository extends JpaRepository<ShiftDetailsView, UUID>, JpaSpecificationExecutor<ShiftDetailsView> {
}
