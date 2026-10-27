package com.faust0z.B2BSystem.orders.infrastructure;

import com.faust0z.B2BSystem.orders.domain.InvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, UUID> {
    Optional<InvoiceEntity> findByOrderId(UUID orderId);
    Optional<InvoiceEntity> findByInvoiceNumber(String invoiceNumber);
}
