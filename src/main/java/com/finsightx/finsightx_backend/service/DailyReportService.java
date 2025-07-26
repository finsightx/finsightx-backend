package com.finsightx.finsightx_backend.service;

import com.finsightx.finsightx_backend.domain.DailyReport;
import com.finsightx.finsightx_backend.domain.PolicyInfo;
import com.finsightx.finsightx_backend.domain.PortfolioItem;
import com.finsightx.finsightx_backend.domain.Stock;
import com.finsightx.finsightx_backend.dto.response.DailyReportListItemResponse;
import com.finsightx.finsightx_backend.dto.response.DailyReportResponse;
import com.finsightx.finsightx_backend.dto.response.PolicyInfoResponse;
import com.finsightx.finsightx_backend.repository.DailyReportRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyReportService {

    private final DailyReportRepository dailyReportRepository;

    private final PolicyInfoService policyInfoService;
    private final StockService stockService;
    private final UserService userService;

    public Optional<DailyReport> getDailyReportById(Long reportId) {
        return dailyReportRepository.findById(reportId);
    }

    public List<DailyReport> getAllDailyReports() {
        return dailyReportRepository.findByOrderByCreatedAtDesc();
    }

    @Transactional
    public void createDailyReport() {
        log.info("Start creating today's daily report.");

        OffsetDateTime todayStart = LocalDate.now(ZoneOffset.ofHours(9)).atStartOfDay().atOffset(ZoneOffset.ofHours(9));
        OffsetDateTime todayEnd = todayStart.plusDays(1).minusNanos(1);

        List<PolicyInfo> todayPolicies = policyInfoService.getPolicyInfosByCreatedAtBetween(todayStart, todayEnd);

        if (todayPolicies.isEmpty()) {
            log.info("No policy information generated today, so the daily report will not be created.");
            return;
        }

        List<Long> policyIds = todayPolicies.stream()
                .map(PolicyInfo::getPolicyId)
                .collect(Collectors.toList());

        String title = LocalDate.now(ZoneOffset.ofHours(9)).format(DateTimeFormatter.ofPattern("M월 d일 일일 정책 리포트"));

        DailyReport report = new DailyReport();
        report.setCreatedAt(OffsetDateTime.now(ZoneOffset.ofHours(9)));
        report.setTitle(title);
        report.setPolicies(policyIds);

        dailyReportRepository.save(report);

        log.info("Today's daily report has been successfully generated and saved. Title: '{}', Number of policies included: {}", title, policyIds.size());
    }

    public List<DailyReport> searchDailyReportsByKeyword(String keyword) {
        return dailyReportRepository.searchDailyReports(keyword);
    }

    public List<DailyReportListItemResponse> getAllDailyReportsAsDto() {
        List<DailyReport> reports = getAllDailyReports();

        return mapReportsToDailyReportListItemResponses(reports);
    }

    public List<DailyReportListItemResponse> getPersonalizedDailyReportsAsDto(Long userId) {
        List<DailyReport> allReports = getAllDailyReports();

        if (allReports.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> userPortfolioStockCodes;
        Set<String> userPortfolioIndustryCodes;
        Map<String, Stock> userStockMap;

        try {
            List<PortfolioItem> userPortfolioItems = userService.getUserPortfolio(userId);

            userPortfolioStockCodes = userPortfolioItems.stream()
                    .map(PortfolioItem::getStockCode)
                    .collect(Collectors.toSet());


            userStockMap = stockService.getStocksByStockCodeIn(new ArrayList<>(userPortfolioStockCodes)).stream()
                    .collect(Collectors.toMap(Stock::getStockCode, stock -> stock));

            userPortfolioIndustryCodes = userStockMap.values().stream()
                    .map(Stock::getIndustryCode)
                    .collect(Collectors.toSet());

        } catch (IllegalArgumentException e) {
            // "User portfolio not found for userId: {userId}"
            userPortfolioStockCodes = Collections.emptySet();
            userPortfolioIndustryCodes = Collections.emptySet();
        }

        final Set<String> finalUserPortfolioStockCodes = userPortfolioStockCodes;
        final Set<String> finalUserPortfolioIndustryCodes = userPortfolioIndustryCodes;

        List<Long> allPolicyIds = allReports.stream()
                .flatMap(report -> report.getPolicies() != null ? report.getPolicies().stream() : Stream.empty())
                .distinct()
                .collect(Collectors.toList());

        Map<Long, PolicyInfo> policyInfoMap = policyInfoService.getPolicyInfoByIds(allPolicyIds).stream()
                .collect(Collectors.toMap(PolicyInfo::getPolicyId, policy -> policy));

        Map<String, String> allIndustryNamesMap = new HashMap<>();
        Map<String, String> allStockNamesMap = new HashMap<>();

        Set<String> distinctAllReportIndustryCodes = new HashSet<>();
        Set<String> distinctAllReportStockCodes = new HashSet<>();

        for (PolicyInfo policy : policyInfoMap.values()) {
            if (policy.getPositiveIndustries() != null) distinctAllReportIndustryCodes.addAll(policy.getPositiveIndustries());
            if (policy.getNegativeIndustries() != null) distinctAllReportIndustryCodes.addAll(policy.getNegativeIndustries());
            if (policy.getPositiveStocks() != null) distinctAllReportStockCodes.addAll(policy.getPositiveStocks());
            if (policy.getNegativeStocks() != null) distinctAllReportStockCodes.addAll(policy.getNegativeStocks());
        }

        if (!distinctAllReportIndustryCodes.isEmpty()) {
            stockService.getStocksByIndustryCodeIn(new ArrayList<>(distinctAllReportIndustryCodes))
                    .forEach(stock -> allIndustryNamesMap.putIfAbsent(stock.getIndustryCode(), stock.getIndustryName()));
        }

        if (!distinctAllReportStockCodes.isEmpty()) {
            stockService.getStocksByStockCodeIn(new ArrayList<>(distinctAllReportStockCodes))
                    .forEach(stock -> allStockNamesMap.putIfAbsent(stock.getStockCode(), stock.getStockName()));
        }


        return allReports.stream()
                .filter(report -> {
                    Set<String> reportRelatedIndustryCodes = new HashSet<>();
                    Set<String> reportRelatedStockCodes = new HashSet<>();

                    if (report.getPolicies() != null) {
                        for (Long policyId : report.getPolicies()) {
                            PolicyInfo policyInfo = policyInfoMap.get(policyId);
                            if (policyInfo != null) {
                                if (policyInfo.getPositiveIndustries() != null) reportRelatedIndustryCodes.addAll(policyInfo.getPositiveIndustries());
                                if (policyInfo.getNegativeIndustries() != null) reportRelatedIndustryCodes.addAll(policyInfo.getNegativeIndustries());
                                if (policyInfo.getPositiveStocks() != null) reportRelatedStockCodes.addAll(policyInfo.getPositiveStocks());
                                if (policyInfo.getNegativeStocks() != null) reportRelatedStockCodes.addAll(policyInfo.getNegativeStocks());
                            }
                        }
                    }

                    boolean industryOverlap = reportRelatedIndustryCodes.stream()
                            .anyMatch(finalUserPortfolioIndustryCodes::contains);

                    boolean stockOverlap = reportRelatedStockCodes.stream()
                            .anyMatch(finalUserPortfolioStockCodes::contains);

                    return industryOverlap || stockOverlap;
                })
                .map(report -> {
                    Set<String> relatedIndustryNames = new HashSet<>();
                    Set<String> userStockNames = new HashSet<>();

                    if (report.getPolicies() != null) {
                        for (Long policyId : report.getPolicies()) {
                            PolicyInfo policyInfo = policyInfoMap.get(policyId);
                            if (policyInfo != null) {
                                Set<String> currentPolicyIndustryCodes = new HashSet<>();
                                if (policyInfo.getPositiveIndustries() != null) currentPolicyIndustryCodes.addAll(policyInfo.getPositiveIndustries());
                                if (policyInfo.getNegativeIndustries() != null) currentPolicyIndustryCodes.addAll(policyInfo.getNegativeIndustries());
                                for (String industryCode : currentPolicyIndustryCodes) {
                                    Optional.ofNullable(allIndustryNamesMap.get(industryCode))
                                            .ifPresent(relatedIndustryNames::add);
                                }

                                Set<String> currentPolicyStockCodes = new HashSet<>();
                                if (policyInfo.getPositiveStocks() != null) currentPolicyStockCodes.addAll(policyInfo.getPositiveStocks());
                                if (policyInfo.getNegativeStocks() != null) currentPolicyStockCodes.addAll(policyInfo.getNegativeStocks());
                                for (String stockCode : currentPolicyStockCodes) {
                                    if (finalUserPortfolioStockCodes.contains(stockCode)) {
                                        Optional.ofNullable(allStockNamesMap.get(stockCode))
                                                .ifPresent(userStockNames::add);
                                    }
                                }
                            }
                        }
                    }
                    return new DailyReportListItemResponse(
                            report.getReportId(),
                            report.getTitle(),
                            report.getCreatedAt(),
                            new ArrayList<>(relatedIndustryNames),
                            new ArrayList<>(userStockNames)
                    );
                })
                .collect(Collectors.toList());
    }

    public Optional<DailyReportResponse> getDailyReportAsDto(Long reportId) {
        return dailyReportRepository.findById(reportId)
                .map(report -> {
                    List<Long> policyIds = report.getPolicies() != null ? report.getPolicies() : new ArrayList<>();
                    Map<Long, PolicyInfo> policyInfoMap = new HashMap<>();

                    if (!policyIds.isEmpty()) {
                        List<PolicyInfo> fetchedPolicyInfos = policyInfoService.getPolicyInfoByIds(policyIds);
                        policyInfoMap = fetchedPolicyInfos.stream()
                                .collect(Collectors.toMap(PolicyInfo::getPolicyId, policy -> policy));
                    }

                    List<PolicyInfoResponse> policyInfoResponses = policyIds.stream()
                            .map(policyInfoMap::get)
                            .filter(java.util.Objects::nonNull)
                            .map(policyInfoService::toPolicyInfoResponse)
                            .collect(Collectors.toList());

                    return new DailyReportResponse(
                            report.getReportId(),
                            report.getTitle(),
                            report.getCreatedAt(),
                            policyInfoResponses
                    );
                });
    }

    public List<DailyReportListItemResponse> searchDailyReportsAsDto(String keyword) {
        List<DailyReport> reports = searchDailyReportsByKeyword(keyword);

        return mapReportsToDailyReportListItemResponses(reports);
    }


    public DailyReportListItemResponse toDailyReportListItemResponse(DailyReport report) {
        Set<String> industryNames = new HashSet<>();
        for (Long policyId : report.getPolicies()) {
            policyInfoService.getPolicyInfoById(policyId).ifPresent(policyInfo -> {
                List<String> allIndustryCodes = new ArrayList<>();
                if (policyInfo.getPositiveIndustries() != null) allIndustryCodes.addAll(policyInfo.getPositiveIndustries());
                if (policyInfo.getNegativeIndustries() != null) allIndustryCodes.addAll(policyInfo.getNegativeIndustries());

                for (String industryCode : allIndustryCodes) {
                    stockService.getIndustryNameByCode(industryCode)
                            .ifPresent(industryNames::add);
                }
            });
        }

        return new DailyReportListItemResponse(
                report.getReportId(),
                report.getTitle(),
                report.getCreatedAt(),
                new ArrayList<>(industryNames),
                new ArrayList<>()
        );
    }

    public DailyReportListItemResponse toDailyReportPersonalizedListItemResponse(DailyReport report, Long userId) {
        Set<String> industryNames = new HashSet<>();
        Set<String> stockNames = new HashSet<>();

        try {
            List<String> userPortfolioStockCodes = userService.getUserPortfolio(userId).stream()
                    .map(PortfolioItem::getStockCode)
                    .toList();

            for (Long policyId : report.getPolicies()) {
                policyInfoService.getPolicyInfoById(policyId).ifPresent(policyInfo -> {
                    List<String> allIndustryCodes = new ArrayList<>();
                    if (policyInfo.getPositiveIndustries() != null) allIndustryCodes.addAll(policyInfo.getPositiveIndustries());
                    if (policyInfo.getNegativeIndustries() != null) allIndustryCodes.addAll(policyInfo.getNegativeIndustries());
                    for (String industryCode : allIndustryCodes) {
                        stockService.getIndustryNameByCode(industryCode)
                                .ifPresent(industryNames::add);
                    }

                    List<String> allStockCodes = new ArrayList<>();
                    if (policyInfo.getPositiveStocks() != null) allStockCodes.addAll(policyInfo.getPositiveStocks());
                    if (policyInfo.getNegativeStocks() != null) allStockCodes.addAll(policyInfo.getNegativeStocks());

                    for (String stockCode : allStockCodes) {
                        if (userPortfolioStockCodes.contains(stockCode)) {
                            stockService.getStockByStockCode(stockCode).ifPresent(stock -> stockNames.add(stock.getStockName()));
                        }
                    }
                });
            }
        } catch (IllegalArgumentException e) {
            // Empty List
        }

        return new DailyReportListItemResponse(
                report.getReportId(),
                report.getTitle(),
                report.getCreatedAt(),
                new ArrayList<>(industryNames),
                new ArrayList<>(stockNames)
        );
    }

    private List<DailyReportListItemResponse> mapReportsToDailyReportListItemResponses(List<DailyReport> reports) {
        if (reports.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> allPolicyIds = reports.stream()
                .flatMap(report -> report.getPolicies() != null ? report.getPolicies().stream() : Stream.empty())
                .distinct()
                .collect(Collectors.toList());

        Map<Long, PolicyInfo> policyInfoMap = policyInfoService.getPolicyInfoByIds(allPolicyIds).stream()
                .collect(Collectors.toMap(PolicyInfo::getPolicyId, policy -> policy));

        List<String> allIndustryCodes = policyInfoMap.values().stream()
                .flatMap(policy -> Stream.concat(
                        policy.getPositiveIndustries() != null ? policy.getPositiveIndustries().stream() : Stream.empty(),
                        policy.getNegativeIndustries() != null ? policy.getNegativeIndustries().stream() : Stream.empty()
                ))
                .distinct()
                .collect(Collectors.toList());

        Map<String, String> industryNamesMap = stockService.getStocksByIndustryCodeIn(allIndustryCodes).stream()
                .collect(Collectors.toMap(Stock::getIndustryCode, Stock::getIndustryName, (existing, replacement) -> existing));

        return reports.stream()
                .map(report -> {
                    Set<String> relatedIndustries = new HashSet<>();
                    if (report.getPolicies() != null) {
                        for (Long policyId : report.getPolicies()) {
                            PolicyInfo policyInfo = policyInfoMap.get(policyId);
                            if (policyInfo != null) {
                                List<String> currentPolicyIndustryCodes = new ArrayList<>();
                                if (policyInfo.getPositiveIndustries() != null) currentPolicyIndustryCodes.addAll(policyInfo.getPositiveIndustries());
                                if (policyInfo.getNegativeIndustries() != null) currentPolicyIndustryCodes.addAll(policyInfo.getNegativeIndustries());

                                for (String industryCode : currentPolicyIndustryCodes) {
                                    Optional.ofNullable(industryNamesMap.get(industryCode))
                                            .ifPresent(relatedIndustries::add);
                                }
                            }
                        }
                    }
                    return new DailyReportListItemResponse(
                            report.getReportId(),
                            report.getTitle(),
                            report.getCreatedAt(),
                            new ArrayList<>(relatedIndustries),
                            new ArrayList<>()
                    );
                })
                .collect(Collectors.toList());
    }

    public DailyReportResponse toDailyReportResponse(DailyReport report) {
        List<Long> policyIds = report.getPolicies() != null ? report.getPolicies() : new ArrayList<>();
        Map<Long, PolicyInfo> policyInfoMap = new HashMap<>();

        if (!policyIds.isEmpty()) {
            List<PolicyInfo> fetchedPolicyInfos = policyInfoService.getPolicyInfoByIds(policyIds);
            policyInfoMap = fetchedPolicyInfos.stream()
                    .collect(Collectors.toMap(PolicyInfo::getPolicyId, policy -> policy));
        }

        List<PolicyInfoResponse> policyInfoResponses = policyIds.stream()
                .map(policyInfoMap::get)
                .filter(java.util.Objects::nonNull)
                .map(policyInfoService::toPolicyInfoResponse)
                .collect(Collectors.toList());

        return new DailyReportResponse(
                report.getReportId(),
                report.getTitle(),
                report.getCreatedAt(),
                policyInfoResponses
        );
    }

}
