package com.faust0z.B2BSystem.orders.infrastructure;

import com.faust0z.B2BSystem.orders.domain.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItemEntity, UUID> {
}
