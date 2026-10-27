package com.faust0z.B2BSystem.staffing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faust0z.B2BSystem.staffing.application.ShiftSweeperJob;
import com.faust0z.B2BSystem.staffing.infrastructure.ShiftRepository;
@ExtendWith(MockitoExtension.class)
class ShiftSweeperJobTest {

    @Mock
    private ShiftRepository shiftRepository;

    @InjectMocks
    private ShiftSweeperJob shiftSweeperJob;

    @Captor
    private ArgumentCaptor<Instant> thresholdCaptor;

    @Captor
    private ArgumentCaptor<Instant> nowCaptor;

    @Test
    void closeExpiredShifts_UnfinishedShiftsExist_ExecutesUpdateQuery() {
        when(shiftRepository.closeUnfinishedShifts(any(Instant.class), any(Instant.class))).thenReturn(5);

        shiftSweeperJob.closeExpiredShifts();

        verify(shiftRepository).closeUnfinishedShifts(thresholdCaptor.capture(), nowCaptor.capture());
        Instant capturedThreshold = thresholdCaptor.getValue();
        Instant capturedNow = nowCaptor.getValue();
        assertThat(Duration.between(capturedThreshold, capturedNow)).isEqualTo(Duration.ofHours(18));
    }

    @Test
    void closeExpiredShifts_NoUnfinishedShiftsExist_SkipsLogging() {
        when(shiftRepository.closeUnfinishedShifts(any(Instant.class), any(Instant.class))).thenReturn(0);

        shiftSweeperJob.closeExpiredShifts();

        verify(shiftRepository).closeUnfinishedShifts(thresholdCaptor.capture(), nowCaptor.capture());
        Instant capturedThreshold = thresholdCaptor.getValue();
        Instant capturedNow = nowCaptor.getValue();
        assertThat(Duration.between(capturedThreshold, capturedNow)).isEqualTo(Duration.ofHours(18));
    }
}
