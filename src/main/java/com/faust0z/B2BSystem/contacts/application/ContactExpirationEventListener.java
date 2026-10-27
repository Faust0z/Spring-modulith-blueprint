package com.faust0z.B2BSystem.contacts.application;

import com.faust0z.B2BSystem.contacts.application.ContactService;
import com.faust0z.B2BSystem.orders.domain.OrderPlacedEvent;
import com.faust0z.B2BSystem.orders.domain.OrderVoidedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ContactExpirationEventListener {

    private final ContactService contactService;

    public ContactExpirationEventListener(ContactService contactService) {
        this.contactService = contactService;
    }

    @EventListener
    void onOrderPlaced(OrderPlacedEvent event) {
        log.debug("Order placed | contactId={}", event.contactId());
        contactService.extendMembership(
                event.contactId(),
                null,
                null,
                event.paidAt()
        );
    }

    @EventListener
    void onOrderVoided(OrderVoidedEvent event) {
        log.debug("Order voided | orderId={} | contactId={}", event.orderId(), event.contactId());
        contactService.reduceMembership(event.contactId(), null);
    }
}
