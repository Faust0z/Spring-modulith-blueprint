package com.faust0z.B2BSystem.staffing.application;

import com.faust0z.B2BSystem.staffing.infrastructure.ShiftRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
public class ShiftSweeperJob {

    private final ShiftRepository shiftRepository;

    public ShiftSweeperJob(ShiftRepository shiftRepository) {
        this.shiftRepository = shiftRepository;
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void closeExpiredShifts() {
        Instant now = Instant.now();
        Instant threshold = now.minus(Duration.ofHours(18));

        int affectedRows = shiftRepository.closeUnfinishedShifts(threshold, now);

        if (affectedRows > 0) {
            log.info("Unfinished staff shifts sweeper executed | closed_count={}", affectedRows);
        }
    }
}
