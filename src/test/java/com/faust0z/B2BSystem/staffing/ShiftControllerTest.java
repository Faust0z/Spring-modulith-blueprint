package com.faust0z.B2BSystem.staffing;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faust0z.B2BSystem.staffing.application.ShiftService;
import com.faust0z.B2BSystem.staffing.domain.StaffIdentifierType;
import com.faust0z.B2BSystem.staffing.web.ShiftController;
import com.faust0z.B2BSystem.staffing.web.ShiftFilterRequest;
import com.faust0z.B2BSystem.staffing.web.ShiftProcessRequest;
import com.faust0z.B2BSystem.staffing.web.ShiftResponse;
@ExtendWith(MockitoExtension.class)
class ShiftControllerTest {

    @Mock
    private ShiftService shiftService;
    @InjectMocks
    private ShiftController shiftController;

    @Nested
    class GetAllShifts {

        @Test
        void getAllShifts_ValidRequest_ReturnsPage() {
            ShiftFilterRequest filters = new ShiftFilterRequest(null, null, null, null,
                    null, null, null);
            PageRequest pageable = PageRequest.of(0, 20);
            Page<ShiftResponse> expectedPage = new PageImpl<>(List.of());
            when(shiftService.getAllShifts(pageable, filters)).thenReturn(expectedPage);

            Page<ShiftResponse> result = shiftController.getAllShifts(filters, pageable);

            assertThat(result).isEqualTo(expectedPage);
            verify(shiftService).getAllShifts(pageable, filters);
        }
    }

    @Nested
    class ProcessShift {

        @Test
        void processShift_ValidRequest_ReturnsResponse() {
            UUID userId = UUID.randomUUID();
            ShiftProcessRequest request = new ShiftProcessRequest(StaffIdentifierType.DNI, "12345678", UUID.randomUUID());
            ShiftResponse expectedResponse = createDefaultResponse(userId);

            when(shiftService.processShift(request)).thenReturn(expectedResponse);

            ShiftResponse result = shiftController.processShift(request);

            assertThat(result).isEqualTo(expectedResponse);
            verify(shiftService).processShift(request);
        }
    }

    @Nested
    class VoidShift {

        @Test
        void voidShift_ValidShiftId_DelegatesToService() {
            UUID shiftId = UUID.randomUUID();

            shiftController.voidShift(shiftId);

            verify(shiftService).voidShift(shiftId);
        }
    }

    private ShiftResponse createDefaultResponse(UUID userId) {
        return new ShiftResponse(UUID.randomUUID(), userId, "John Doe", Instant.now(), null, false,
                true, UUID.randomUUID());
    }
}
