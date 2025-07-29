package com.finsightx.finsightx_backend.scheduler;

import com.finsightx.finsightx_backend.service.DailyReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyReportScheduller {

    private final DailyReportService dailyReportService;

    @Scheduled(cron = "0 0 21 * * *", zone = "Asia/Seoul")
    public void runDailyReportGeneration() {
        log.info("Scheduler is starting to create daily report.");
        dailyReportService.createDailyReport(LocalDate.now(ZoneOffset.ofHours(9)));
    }

}
