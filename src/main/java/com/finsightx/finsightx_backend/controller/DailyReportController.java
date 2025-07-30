package com.finsightx.finsightx_backend.controller;

import com.finsightx.finsightx_backend.dto.response.DailyReportListItemResponse;
import com.finsightx.finsightx_backend.dto.response.DailyReportResponse;
import com.finsightx.finsightx_backend.service.DailyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/daily-report")
@RequiredArgsConstructor
public class DailyReportController {

    private final DailyReportService dailyReportService;

    @GetMapping("/all")
    public ResponseEntity<List<DailyReportListItemResponse>> getAllDailyReports() {
        List<DailyReportListItemResponse> responses = dailyReportService.getAllDailyReportsAsDto();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/personalized/{userId}")
    public ResponseEntity<List<DailyReportListItemResponse>> getPersonalizedDailyReports(@PathVariable Long userId) {
        List<DailyReportListItemResponse> responses = dailyReportService.getPersonalizedDailyReportsAsDto(userId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<DailyReportResponse> getDailyReportById(@PathVariable Long reportId) {
        return dailyReportService.getDailyReportAsDto(reportId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<DailyReportListItemResponse>> searchDailyReports(@RequestParam String keyword) {
        List<DailyReportListItemResponse> response = dailyReportService.searchDailyReportsAsDto(keyword);
        return ResponseEntity.ok(response);
    }

}
