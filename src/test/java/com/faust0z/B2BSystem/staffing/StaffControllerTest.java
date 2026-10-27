package com.faust0z.B2BSystem.staffing;

import com.faust0z.B2BSystem.core.TenantContext;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faust0z.B2BSystem.staffing.application.StaffService;
import com.faust0z.B2BSystem.staffing.web.StaffController;
import com.faust0z.B2BSystem.staffing.web.StaffCreateRequest;
import com.faust0z.B2BSystem.staffing.web.StaffResponse;
import com.faust0z.B2BSystem.staffing.web.StaffUpdateRequest;
@ExtendWith(MockitoExtension.class)
class StaffControllerTest {

    @Mock
    private StaffService staffService;

    @InjectMocks
    private StaffController staffController;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Nested
    class GetStaff {

        @Test
        void getStaff_StaffExists_ReturnsList() {
            StaffResponse response = mock(StaffResponse.class);
            when(staffService.getStaff()).thenReturn(List.of(response));

            List<StaffResponse> result = staffController.getStaff();

            assertThat(result).hasSize(1).contains(response);
            verify(staffService).getStaff();
        }
    }

    @Nested
    class CreateStaff {

        @Test
        void createStaff_ValidRequest_ReturnsCreatedStaff() {
            StaffCreateRequest request = mock(StaffCreateRequest.class);
            StaffResponse response = mock(StaffResponse.class);
            when(staffService.createStaff(request)).thenReturn(response);

            StaffResponse result = staffController.createStaff(request);

            assertThat(result).isEqualTo(response);
            verify(staffService).createStaff(request);
        }
    }

    @Nested
    class UpdateStaff {

        @Test
        void updateStaff_ValidRequest_ReturnsUpdatedStaff() {
            UUID staffId = UUID.randomUUID();
            StaffUpdateRequest request = mock(StaffUpdateRequest.class);
            StaffResponse response = mock(StaffResponse.class);
            when(staffService.updateStaff(staffId, request)).thenReturn(response);

            StaffResponse result = staffController.updateStaff(staffId, request);

            assertThat(result).isEqualTo(response);
            verify(staffService).updateStaff(staffId, request);
        }
    }

    @Nested
    class ToggleStaffStatus {

        @Test
        void toggleStaffStatus_StaffExists_ReturnsToggledStaff() {
            UUID staffId = UUID.randomUUID();
            StaffResponse response = mock(StaffResponse.class);
            when(staffService.toggleStaffStatus(staffId)).thenReturn(response);

            StaffResponse result = staffController.toggleStaffStatus(staffId);

            assertThat(result).isEqualTo(response);
            verify(staffService).toggleStaffStatus(staffId);
        }
    }
}
