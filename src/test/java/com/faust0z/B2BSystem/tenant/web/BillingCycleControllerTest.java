package com.faust0z.B2BSystem.tenant.web;

import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.tenant.application.BillingCycleService;
import com.faust0z.B2BSystem.tenant.domain.CycleStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.faust0z.B2BSystem.tenant.web.BillingCycleController;
import com.faust0z.B2BSystem.tenant.web.BillingCyclePayFirstNonDemoRequest;
import com.faust0z.B2BSystem.tenant.web.BillingCyclePayRequest;
import com.faust0z.B2BSystem.tenant.web.BillingCycleResponse;
@ExtendWith(MockitoExtension.class)
class BillingCycleControllerTest {

    @Mock
    private BillingCycleService billingCycleService;

    @InjectMocks
    private BillingCycleController billingCycleController;

    private MockMvc buildMockMvc() {
        return MockMvcBuilders.standaloneSetup(billingCycleController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private BillingCycleResponse createDefaultResponse(UUID id) {
        return new BillingCycleResponse(
                id,
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-31T23:59:59Z"),
                150,
                10,
                50000,
                CycleStatus.PAID,
                false
        );
    }

    @Nested
    class FindAll {

        @Test
        void findAll_HasData_Returns200AndPagedCycles() throws Exception {
            MockMvc mockMvc = buildMockMvc();
            UUID cycleId = UUID.randomUUID();
            BillingCycleResponse response = createDefaultResponse(cycleId);
            Page<BillingCycleResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);

            when(billingCycleService.findAll(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/v1/billing-cycles")
                            .param("page", "0")
                            .param("size", "20")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(cycleId.toString()))
                    .andExpect(jsonPath("$.content[0].peakMembers").value(150))
                    .andExpect(jsonPath("$.content[0].amount").value(50000));
        }
    }

    @Nested
    class FindById {
        @Test
        void findById_CycleExists_Returns200AndCycle() throws Exception {
            MockMvc mockMvc = buildMockMvc();
            UUID cycleId = UUID.randomUUID();
            BillingCycleResponse response = createDefaultResponse(cycleId);

            when(billingCycleService.findById(cycleId)).thenReturn(response);

            mockMvc.perform(get("/v1/billing-cycles/{billingCycleId}", cycleId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(cycleId.toString()))
                    .andExpect(jsonPath("$.status").value("PAID"))
                    .andExpect(jsonPath("$.isDemo").value(false));
        }
    }

    @Nested
    class PayCycle {

        @Test
        void payCycle_ValidPayload_DelegatesAndReturnsResponse() throws Exception {
            MockMvc mockMvc = buildMockMvc();
            UUID tenantId = UUID.randomUUID();
            UUID cycleId = UUID.randomUUID();
            BillingCycleResponse response = createDefaultResponse(cycleId);

            when(billingCycleService.payCycle(any(BillingCyclePayRequest.class))).thenReturn(response);

            mockMvc.perform(post("/v1/billing-cycles/pay")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tenantId\":\"" + tenantId + "\",\"cycleId\":\"" + cycleId + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(cycleId.toString()))
                    .andExpect(jsonPath("$.status").value("PAID"));
        }

        @Test
        void payCycle_InvalidPayload_Returns400() throws Exception {
            MockMvc mockMvc = buildMockMvc();

            mockMvc.perform(post("/v1/billing-cycles/pay")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class PayFirstCycle {

        @Test
        void payFirstCycle_ValidPayload_DelegatesAndReturnsResponse() throws Exception {
            MockMvc mockMvc = buildMockMvc();
            UUID tenantId = UUID.randomUUID();
            UUID planId = UUID.randomUUID();
            UUID cycleId = UUID.randomUUID();
            BillingCycleResponse response = createDefaultResponse(cycleId);

            when(billingCycleService.payFirstCycle(any(BillingCyclePayFirstNonDemoRequest.class))).thenReturn(response);

            mockMvc.perform(post("/v1/billing-cycles/pay-first")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tenantId\":\"" + tenantId + "\",\"planId\":\"" + planId + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(cycleId.toString()))
                    .andExpect(jsonPath("$.status").value("PAID"));
        }

        @Test
        void payFirstCycle_MissingTenant_Returns400() throws Exception {
            MockMvc mockMvc = buildMockMvc();

            mockMvc.perform(post("/v1/billing-cycles/pay-first")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
