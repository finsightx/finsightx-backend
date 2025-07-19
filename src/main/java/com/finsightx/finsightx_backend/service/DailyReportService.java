package com.finsightx.finsightx_backend.service;

import com.finsightx.finsightx_backend.domain.DailyReport;
import com.finsightx.finsightx_backend.repository.DailyReportRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DailyReportService {

    private final DailyReportRepository dailyReportRepository;

    public Optional<DailyReport> getDailyReportById(Long reportId) {
        return dailyReportRepository.findById(reportId);
    }

    public List<DailyReport> getAllDailyReports() {
        return dailyReportRepository.findByOrderByCreatedAtDesc();
    }

    // TODO: Check
    @Transactional
    public DailyReport saveDailyReport(DailyReport report) {
        if (report.getCreatedAt() == null) {
            report.setCreatedAt(OffsetDateTime.now());
        }
        return dailyReportRepository.save(report);
    }

    public List<DailyReport> searchDailyReportsByKeyword(String keyword) {
        return dailyReportRepository.searchDailyReports(keyword);
    }

}
