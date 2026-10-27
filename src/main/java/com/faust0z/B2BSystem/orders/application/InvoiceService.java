package com.faust0z.B2BSystem.orders.application;

import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.orders.domain.InvoiceEntity;
import com.faust0z.B2BSystem.orders.domain.InvoiceNotFoundException;
import com.faust0z.B2BSystem.orders.domain.InvoiceStatus;
import com.faust0z.B2BSystem.orders.domain.OrderNotFoundException;
import com.faust0z.B2BSystem.orders.infrastructure.InvoiceRepository;
import com.faust0z.B2BSystem.orders.infrastructure.OrderRepository;
import com.faust0z.B2BSystem.orders.web.InvoiceCreateRequest;
import com.faust0z.B2BSystem.orders.web.InvoiceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByOrderId(UUID orderId) {
        return invoiceRepository.findByOrderId(orderId)
                .map(this::toResponse)
                .orElseThrow(InvoiceNotFoundException::new);
    }

    @Transactional
    public InvoiceResponse createInvoice(InvoiceCreateRequest request) {
        var order = orderRepository.findById(request.orderId())
                .orElseThrow(OrderNotFoundException::new);

        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setTenantId(TenantContext.getTenantId());
        invoice.setOrderId(order.getId());
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setIssueDate(Instant.now());
        invoice.setDueDate(request.dueDate());
        invoice.setTotalAmount(order.getTotalAmount());
        invoice.setStatus(InvoiceStatus.ISSUED);

        invoice = invoiceRepository.save(invoice);
        log.info("Invoice created | invoiceId={} | orderId={}", invoice.getId(), order.getId());
        return toResponse(invoice);
    }

    private String generateInvoiceNumber() {
        return "INV-" + Instant.now().toEpochMilli();
    }

    private InvoiceResponse toResponse(InvoiceEntity entity) {
        return new InvoiceResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getOrderId(),
                entity.getInvoiceNumber(),
                entity.getIssueDate(),
                entity.getDueDate(),
                entity.getTotalAmount(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
