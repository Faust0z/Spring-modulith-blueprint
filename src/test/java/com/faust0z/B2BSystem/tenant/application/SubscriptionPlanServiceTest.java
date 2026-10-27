package com.faust0z.B2BSystem.tenant.application;

import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanEntity;
import com.faust0z.B2BSystem.tenant.domain.SubscriptionPlanNotFoundException;
import com.faust0z.B2BSystem.tenant.infrastructure.SubscriptionPlanRepository;
import com.faust0z.B2BSystem.tenant.web.SubscriptionPlanCreateRequest;
import com.faust0z.B2BSystem.tenant.web.SubscriptionPlanResponse;
import com.faust0z.B2BSystem.tenant.web.SubscriptionPlanUpdateRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faust0z.B2BSystem.tenant.application.SubscriptionPlanMapper;
import com.faust0z.B2BSystem.tenant.application.SubscriptionPlanService;
@ExtendWith(MockitoExtension.class)
class SubscriptionPlanServiceTest {

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Mock
    private SubscriptionPlanMapper subscriptionPlanMapper;

    @InjectMocks
    private SubscriptionPlanService service;

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
    }

    @Nested
    class FindAll {

        @Test
        void findAll_DataExists_ReturnsMappedList() {
            List<SubscriptionPlanResponse> expected = List.of(createDefaultResponse(UUID.randomUUID()));
            when(subscriptionPlanRepository.findBy(SubscriptionPlanResponse.class)).thenReturn(expected);

            List<SubscriptionPlanResponse> result = service.findAll();

            assertThat(result).isEqualTo(expected);
        }
    }

    @Nested
    class FindById {

        @Test
        void findById_PlanExists_ReturnsResponse() {
            UUID planId = UUID.randomUUID();
            SubscriptionPlanResponse expected = createDefaultResponse(planId);
            when(subscriptionPlanRepository.findById(planId, SubscriptionPlanResponse.class)).thenReturn(Optional.of(expected));

            SubscriptionPlanResponse result = service.findById(planId);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        void findById_PlanMissing_ThrowsException() {
            UUID planId = UUID.randomUUID();
            when(subscriptionPlanRepository.findById(planId, SubscriptionPlanResponse.class)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(planId))
                    .isInstanceOf(SubscriptionPlanNotFoundException.class);
        }
    }

    @Nested
    class Create {

        @Test
        void create_ValidRequest_MapsSavesAndReturnsResponse() {
            SubscriptionPlanCreateRequest request = new SubscriptionPlanCreateRequest("PREMIUM", 50000, 250);
            SubscriptionPlanEntity entity = new SubscriptionPlanEntity();
            SubscriptionPlanResponse expectedResponse = createDefaultResponse(UUID.randomUUID());

            when(subscriptionPlanMapper.toEntity(request)).thenReturn(entity);
            when(subscriptionPlanRepository.save(entity)).thenReturn(entity);
            when(subscriptionPlanMapper.toResponse(entity)).thenReturn(expectedResponse);

            SubscriptionPlanResponse result = service.create(request);

            assertThat(result).isEqualTo(expectedResponse);
            verify(subscriptionPlanMapper).toEntity(request);
            verify(subscriptionPlanRepository).save(entity);
            verify(subscriptionPlanMapper).toResponse(entity);
        }
    }

    @Nested
    class Update {

        @Test
        void update_PlanExists_UpdatesEntityAndReturnsResponse() {
            UUID planId = UUID.randomUUID();
            SubscriptionPlanUpdateRequest request = new SubscriptionPlanUpdateRequest("BASIC", 25000, 500);
            SubscriptionPlanEntity existingEntity = new SubscriptionPlanEntity();
            existingEntity.setId(planId);
            SubscriptionPlanResponse expectedResponse = createDefaultResponse(planId);

            when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(existingEntity));
            when(subscriptionPlanMapper.toResponse(existingEntity)).thenReturn(expectedResponse);

            SubscriptionPlanResponse result = service.update(planId, request);

            assertThat(result).isEqualTo(expectedResponse);
            verify(subscriptionPlanMapper).updateFromRequest(request, existingEntity);
            verify(subscriptionPlanMapper).toResponse(existingEntity);
        }

        @Test
        void update_PlanMissing_ThrowsException() {
            UUID planId = UUID.randomUUID();
            SubscriptionPlanUpdateRequest request = new SubscriptionPlanUpdateRequest("BASIC", 25000, 500);
            when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(planId, request))
                    .isInstanceOf(SubscriptionPlanNotFoundException.class);
        }
    }

    private SubscriptionPlanResponse createDefaultResponse(UUID id) {
        return new SubscriptionPlanResponse(
                id,
                "PREMIUM",
                50000,
                250
        );
    }
}