package com.faust0z.B2BSystem.staffing.application;

import com.faust0z.B2BSystem.core.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faust0z.B2BSystem.staffing.domain.StaffEntity;
import com.faust0z.B2BSystem.staffing.domain.StaffIdentifierType;
import com.faust0z.B2BSystem.staffing.infrastructure.StaffRepository;
import com.faust0z.B2BSystem.staffing.web.ShiftProcessRequest;
import com.faust0z.B2BSystem.staffing.web.StaffCreateRequest;
import com.faust0z.B2BSystem.staffing.web.StaffDisabledException;
import com.faust0z.B2BSystem.staffing.web.StaffNotFoundException;
import com.faust0z.B2BSystem.staffing.web.StaffResponse;
import com.faust0z.B2BSystem.staffing.web.StaffUpdateRequest;
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StaffServiceTest {

    @Mock
    private StaffRepository staffRepository;
    @Mock
    private StaffMapper staffMapper;

    @InjectMocks
    private StaffService staffService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Nested
    class GetStaff {

        @Test
        void getStaff_StaffExists_ReturnsList() {
            StaffResponse response = mock(StaffResponse.class);
            when(staffRepository.findBy(StaffResponse.class)).thenReturn(List.of(response));

            List<StaffResponse> result = staffService.getStaff();

            assertThat(result).hasSize(1).contains(response);
        }
    }

    @Nested
    class GetValidStaff {

        @Test
        void getValidStaff_DniExistsAndActive_ReturnsStaff() {
            String dni = "12345678";
            ShiftProcessRequest request = new ShiftProcessRequest(StaffIdentifierType.DNI, dni, UUID.randomUUID());
            StaffResponse response = mock(StaffResponse.class);
            when(response.deletedAt()).thenReturn(null);
            when(staffRepository.findByDni(dni, StaffResponse.class)).thenReturn(Optional.of(response));

            StaffResponse result = staffService.getValidStaff(request);

            assertThat(result).isEqualTo(response);
        }

        @Test
        void getValidStaff_DniDoesNotExist_ThrowsStaffNotFoundException() {
            String dni = "12345678";
            ShiftProcessRequest request = new ShiftProcessRequest(StaffIdentifierType.DNI, dni, UUID.randomUUID());
            when(staffRepository.findByDni(dni, StaffResponse.class)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> staffService.getValidStaff(request))
                    .isInstanceOf(StaffNotFoundException.class);
        }

        @Test
        void getValidStaff_UuidExistsAndActive_ReturnsStaff() {
            UUID id = UUID.randomUUID();
            ShiftProcessRequest request = new ShiftProcessRequest(StaffIdentifierType.UUID, id.toString(), UUID.randomUUID());
            StaffResponse response = mock(StaffResponse.class);
            when(response.deletedAt()).thenReturn(null);
            when(staffRepository.findById(id, StaffResponse.class)).thenReturn(Optional.of(response));

            StaffResponse result = staffService.getValidStaff(request);

            assertThat(result).isEqualTo(response);
        }

        @Test
        void getValidStaff_UuidDoesNotExist_ThrowsStaffNotFoundException() {
            UUID id = UUID.randomUUID();
            ShiftProcessRequest request = new ShiftProcessRequest(StaffIdentifierType.UUID, id.toString(), UUID.randomUUID());
            when(staffRepository.findById(id, StaffResponse.class)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> staffService.getValidStaff(request))
                    .isInstanceOf(StaffNotFoundException.class);
        }

        @Test
        void getValidStaff_StaffDisabled_ThrowsStaffDisabledException() {
            String dni = "12345678";
            ShiftProcessRequest request = new ShiftProcessRequest(StaffIdentifierType.DNI, dni, UUID.randomUUID());
            StaffResponse response = mock(StaffResponse.class);
            when(response.deletedAt()).thenReturn(java.time.Instant.now());
            when(staffRepository.findByDni(dni, StaffResponse.class)).thenReturn(Optional.of(response));

            assertThatThrownBy(() -> staffService.getValidStaff(request))
                    .isInstanceOf(StaffDisabledException.class);
        }
    }

    @Nested
    class CreateStaff {

        @Test
        void createStaff_ValidRequest_ReturnsSavedStaff() {
            StaffCreateRequest request = mock(StaffCreateRequest.class);
            StaffEntity entity = new StaffEntity();
            StaffResponse response = mock(StaffResponse.class);

            when(staffMapper.toEntity(request)).thenReturn(entity);
            when(staffRepository.save(entity)).thenReturn(entity);
            when(staffMapper.toResponse(entity)).thenReturn(response);

            StaffResponse result = staffService.createStaff(request);

            assertThat(result).isEqualTo(response);
            verify(staffRepository).save(entity);
        }
    }

    @Nested
    class UpdateStaff {

        @Test
        void updateStaff_StaffExists_ReturnsUpdatedStaff() {
            UUID id = UUID.randomUUID();
            StaffUpdateRequest request = mock(StaffUpdateRequest.class);
            StaffEntity entity = new StaffEntity();
            StaffResponse response = mock(StaffResponse.class);

            when(staffRepository.findById(id)).thenReturn(Optional.of(entity));
            when(staffMapper.toResponse(entity)).thenReturn(response);

            StaffResponse result = staffService.updateStaff(id, request);

            assertThat(result).isEqualTo(response);
            verify(staffMapper).updateFromRequest(request, entity);
        }

        @Test
        void updateStaff_StaffDoesNotExist_ThrowsStaffNotFoundException() {
            UUID id = UUID.randomUUID();
            StaffUpdateRequest request = mock(StaffUpdateRequest.class);
            when(staffRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> staffService.updateStaff(id, request))
                    .isInstanceOf(StaffNotFoundException.class);
        }
    }

    @Nested
    class ToggleStaffStatus {

        @Test
        void toggleStaffStatus_StaffActive_TogglesToInactiveAndReturns() {
            UUID id = UUID.randomUUID();
            StaffEntity entity = new StaffEntity();
            entity.setDeletedAt(null);
            StaffResponse response = mock(StaffResponse.class);

            when(staffRepository.findById(id)).thenReturn(Optional.of(entity));
            when(staffMapper.toResponse(entity)).thenReturn(response);

            StaffResponse result = staffService.toggleStaffStatus(id);

            assertThat(entity.getDeletedAt()).isNotNull();
            assertThat(result).isEqualTo(response);
        }

        @Test
        void toggleStaffStatus_StaffInactive_TogglesToActiveAndReturns() {
            UUID id = UUID.randomUUID();
            StaffEntity entity = new StaffEntity();
            entity.setDeletedAt(java.time.Instant.now());
            StaffResponse response = mock(StaffResponse.class);

            when(staffRepository.findById(id)).thenReturn(Optional.of(entity));
            when(staffMapper.toResponse(entity)).thenReturn(response);

            StaffResponse result = staffService.toggleStaffStatus(id);

            assertThat(entity.getDeletedAt()).isNull();
            assertThat(result).isEqualTo(response);
        }

        @Test
        void toggleStaffStatus_StaffDoesNotExist_ThrowsStaffNotFoundException() {
            UUID id = UUID.randomUUID();
            when(staffRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> staffService.toggleStaffStatus(id))
                    .isInstanceOf(StaffNotFoundException.class);
        }
    }

    @Nested
    class GetStaffStatus {

        @Test
        void getStaffStatus_StaffExists_ReturnsOptionalActiveStatus() {
            String dni = "12345678";
            StaffEntity staffEntity = mock(StaffEntity.class);
            when(staffEntity.getDeletedAt()).thenReturn(null);
            when(staffRepository.findByDni(dni, StaffEntity.class)).thenReturn(Optional.of(staffEntity));

            Optional<Boolean> result = staffService.getStaffStatus(dni);

            assertThat(result).contains(true);
        }

        @Test
        void getStaffStatus_StaffDoesNotExist_ReturnsOptionalEmpty() {
            String dni = "12345678";
            when(staffRepository.findByDni(dni, StaffEntity.class)).thenReturn(Optional.empty());

            Optional<Boolean> result = staffService.getStaffStatus(dni);

            assertThat(result).isEmpty();
        }
    }
}