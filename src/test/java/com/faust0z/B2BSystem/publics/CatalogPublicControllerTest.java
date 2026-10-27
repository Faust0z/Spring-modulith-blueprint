package com.faust0z.B2BSystem.publics;

import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.access.AccessLogInternalApi;
import com.faust0z.B2BSystem.catalog.CatalogTemplateInternalApi;
import com.faust0z.B2BSystem.contacts.ContactInternalApi;
import com.faust0z.B2BSystem.tenant.TenantInternalApi;
import com.faust0z.B2BSystem.tenant.TenantInternalResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.faust0z.B2BSystem.publics.CatalogPublicController;
@ExtendWith(MockitoExtension.class)
class CatalogPublicControllerTest {

    @Mock
    private ContactInternalApi contactInternalApi;
    @Mock
    private TenantInternalApi tenantInternalApi;
    @Mock
    private CatalogTemplateInternalApi catalogTemplateInternalApi;
    @Mock
    private AccessLogInternalApi accessLogInternalApi;

    @InjectMocks
    private CatalogPublicController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Nested
    class GetTemplateCatalog {
        @Test
        void getList() throws Exception {
            String slug = "acme-gym";
            UUID tenantId = UUID.randomUUID();
            TenantInternalResponse internal = mock(TenantInternalResponse.class);
            when(internal.id()).thenReturn(tenantId);

            when(tenantInternalApi.getTenantBySlug(slug)).thenReturn(internal);
            when(catalogTemplateInternalApi.findActiveSummaries()).thenReturn(List.of());

            mockMvc.perform(get("/v1/public/catalog/{slug}/templates", slug))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());

            verify(catalogTemplateInternalApi).findActiveSummaries();
        }
    }
}

