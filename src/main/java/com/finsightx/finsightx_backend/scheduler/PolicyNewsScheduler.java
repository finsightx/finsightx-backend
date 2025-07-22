package com.finsightx.finsightx_backend.scheduler;

import com.finsightx.finsightx_backend.service.PolicyNewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;


@Component
@RequiredArgsConstructor
@Slf4j
public class PolicyNewsScheduler {

    private final PolicyNewsService policyNewsService;

    @Scheduled(cron = "0 */1 7-21 * * MON-FRI", zone = "Asia/Seoul")
    public void schedulePolicyNewsCollection() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalTime currentTime = now.toLocalTime();
        DayOfWeek currentDay = now.getDayOfWeek();

        boolean isWeekday = currentDay != DayOfWeek.SATURDAY && currentDay != DayOfWeek.SUNDAY;
        boolean isWithinOperatingHours = !currentTime.isBefore(LocalTime.of(7, 0)) && !currentTime.isAfter(LocalTime.of(21, 0));

        if (isWeekday && isWithinOperatingHours) {
            log.info("Execute collecting policy news and processing scheduler: {}", now);
            policyNewsService.processPolicyNews();
        } else {
            log.debug("Policy news scheduler: Skip due to off-hours/day. Current: {}", now);
        }
    }

}
