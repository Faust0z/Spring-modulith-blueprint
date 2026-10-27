package com.faust0z.B2BSystem.orders.infrastructure;

import com.faust0z.B2BSystem.orders.web.OrderResponse;
import jakarta.persistence.Entity;

import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "v_order_details")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class OrderDetailsView {
    @Id
    private UUID id;
    @TenantId
    private UUID tenantId;
    private UUID contactId;
    private UUID locationId;
    private String contactFirstName;
    private String contactLastName;
    private String contactDni;
    private String locationName;
    private BigDecimal totalAmount;
    private Instant paidAt;
    private Boolean isVoided;

    public OrderResponse toResponse() {
        String contactName = (contactFirstName != null) ? contactFirstName + " " + contactLastName : "Unknown Contact";
        return new OrderResponse(id, contactId, locationId, null, totalAmount, paidAt, List.of(), isVoided);
    }
}
