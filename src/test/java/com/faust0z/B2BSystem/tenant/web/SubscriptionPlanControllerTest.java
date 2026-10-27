package com.faust0z.B2BSystem.tenant.web;

import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.tenant.application.SubscriptionPlanService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faust0z.B2BSystem.tenant.web.SubscriptionPlanController;
import com.faust0z.B2BSystem.tenant.web.SubscriptionPlanCreateRequest;
import com.faust0z.B2BSystem.tenant.web.SubscriptionPlanResponse;
import com.faust0z.B2BSystem.tenant.web.SubscriptionPlanUpdateRequest;
@ExtendWith(MockitoExtension.class)
class SubscriptionPlanControllerTest {

    @Mock
    private SubscriptionPlanService subscriptionPlanService;

    @InjectMocks
    private SubscriptionPlanController controller;

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
    }

    @Nested
    class FindAll {

        @Test
        void findAll_ReturnsMappedList() {
            List<SubscriptionPlanResponse> expectedList = List.of(createDefaultResponse());
            when(subscriptionPlanService.findAll()).thenReturn(expectedList);

            List<SubscriptionPlanResponse> result = controller.findAll();

            assertThat(result).isEqualTo(expectedList);
            verify(subscriptionPlanService).findAll();
        }
    }

    @Nested
    class FindById {

        @Test
        void findById_DelegatesToService_ReturnsResponse() {
            UUID planId = UUID.randomUUID();
            SubscriptionPlanResponse expectedResponse = createDefaultResponse();
            when(subscriptionPlanService.findById(planId)).thenReturn(expectedResponse);

            SubscriptionPlanResponse result = controller.findById(planId);

            assertThat(result).isEqualTo(expectedResponse);
            verify(subscriptionPlanService).findById(planId);
        }
    }

    @Nested
    class Create {

        @Test
        void create_ValidRequest_ReturnsCreatedResponse() {
            SubscriptionPlanCreateRequest request = new SubscriptionPlanCreateRequest("PREMIUM", 50000, 250);
            SubscriptionPlanResponse expectedResponse = createDefaultResponse();
            when(subscriptionPlanService.create(request)).thenReturn(expectedResponse);

            SubscriptionPlanResponse result = controller.create(request);

            assertThat(result).isEqualTo(expectedResponse);
            verify(subscriptionPlanService).create(request);
        }
    }

    @Nested
    class Update {

        @Test
        void update_ValidRequest_ReturnsUpdatedResponse() {
            UUID planId = UUID.randomUUID();
            SubscriptionPlanUpdateRequest request = new SubscriptionPlanUpdateRequest("BASIC", 25000, 500);
            SubscriptionPlanResponse expectedResponse = createDefaultResponse();
            when(subscriptionPlanService.update(planId, request)).thenReturn(expectedResponse);

            SubscriptionPlanResponse result = controller.update(planId, request);

            assertThat(result).isEqualTo(expectedResponse);
            verify(subscriptionPlanService).update(planId, request);
        }
    }

    private SubscriptionPlanResponse createDefaultResponse() {
        return new SubscriptionPlanResponse(
                UUID.randomUUID(),
                "PREMIUM",
                50000,
                250
        );
    }
}