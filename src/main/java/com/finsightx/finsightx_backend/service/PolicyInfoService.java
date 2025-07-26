package com.finsightx.finsightx_backend.service;

import com.finsightx.finsightx_backend.domain.PolicyInfo;
import com.finsightx.finsightx_backend.domain.Stock;
import com.finsightx.finsightx_backend.dto.response.IndustryResponse;
import com.finsightx.finsightx_backend.dto.response.PolicyInfoResponse;
import com.finsightx.finsightx_backend.dto.response.StockResponse;
import com.finsightx.finsightx_backend.repository.PolicyInfoRepository;
import com.finsightx.finsightx_backend.repository.StockRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PolicyInfoService {

    private final PolicyInfoRepository policyInfoRepository;
    private final StockRepository stockRepository;

    public Optional<PolicyInfo> getPolicyInfoById(Long policyId) {
        return policyInfoRepository.findById(policyId);
    }

    public List<PolicyInfo> getPolicyInfoByIds(List<Long> policyIds) {
        if (policyIds == null || policyIds.isEmpty()) {
            return Collections.emptyList();
        }
        return policyInfoRepository.findByPolicyIdIn(policyIds);
    }

    public List<PolicyInfo> getPolicyInfosByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end) {
        return policyInfoRepository.findByCreatedAtBetween(start, end);
    }

    @Transactional
    public PolicyInfo savePolicyInfo(PolicyInfo policyInfo) {
        if (policyInfo.getCreatedAt() == null) {
            policyInfo.setCreatedAt(OffsetDateTime.now(ZoneId.of("Asia/Seoul")));
        }
        return policyInfoRepository.save(policyInfo);
    }

    public PolicyInfoResponse toPolicyInfoResponse(PolicyInfo policyInfo) {
        if (policyInfo == null) return null;

        List<String> allIndustryCodes = Stream.concat(
                policyInfo.getPositiveIndustries() != null ? policyInfo.getPositiveIndustries().stream() : Stream.empty(),
                policyInfo.getNegativeIndustries() != null ? policyInfo.getNegativeIndustries().stream() : Stream.empty()
        ).distinct().collect(Collectors.toList());

        List<String> allStockCodes = Stream.concat(
                policyInfo.getPositiveStocks() != null ? policyInfo.getPositiveStocks().stream() : Stream.empty(),
                policyInfo.getNegativeStocks() != null ? policyInfo.getNegativeStocks().stream() : Stream.empty()
        ).distinct().collect(Collectors.toList());

        Map<String, String> industryNamesMap = stockRepository.findByIndustryCodeIn(allIndustryCodes).stream()
                .collect(Collectors.toMap(Stock::getIndustryCode, Stock::getIndustryName, (existing, replacement) -> existing)); // 중복 키 처리

        Map<String, String> stockNamesMap = stockRepository.findByStockCodeIn(allStockCodes).stream()
                .collect(Collectors.toMap(Stock::getStockCode, Stock::getStockName));

        // Creating and Mapping DTO
        Set<IndustryResponse> positiveIndustriesSet = new HashSet<>();
        // TODO: Check
        if (policyInfo.getPositiveIndustries() != null) {
            for (String code : policyInfo.getPositiveIndustries()) {
                Optional.ofNullable(industryNamesMap.get(code))
                        .ifPresent(name -> positiveIndustriesSet.add(new IndustryResponse(code, name)));
            }
        }
        List<IndustryResponse> positiveIndustries = new ArrayList<>(positiveIndustriesSet); // Set -> List

        Set<IndustryResponse> negativeIndustriesSet = new HashSet<>();
        if (policyInfo.getNegativeIndustries() != null) {
            for (String code : policyInfo.getNegativeIndustries()) {
                Optional.ofNullable(industryNamesMap.get(code))
                        .ifPresent(name -> negativeIndustriesSet.add(new IndustryResponse(code, name)));
            }
        }
        List<IndustryResponse> negativeIndustries = new ArrayList<>(negativeIndustriesSet); // Set -> List

        List<StockResponse> positiveStocks = new ArrayList<>();
        if (policyInfo.getPositiveStocks() != null) {
            for (String code : policyInfo.getPositiveStocks()) {
                Optional.ofNullable(stockNamesMap.get(code))
                        .ifPresent(name -> positiveStocks.add(new StockResponse(code, name)));
            }
        }

        List<StockResponse> negativeStocks = new ArrayList<>();
        if (policyInfo.getNegativeStocks() != null) {
            for (String code : policyInfo.getNegativeStocks()) {
                Optional.ofNullable(stockNamesMap.get(code))
                        .ifPresent(name -> negativeStocks.add(new StockResponse(code, name)));
            }
        }

        return new PolicyInfoResponse(
                policyInfo.getPolicyId(),
                policyInfo.getPolicyName(),
                policyInfo.getStage(),
                policyInfo.getCreatedAt(),
                policyInfo.getSummary(),
                policyInfo.getContent(),
                positiveIndustries,
                negativeIndustries,
                positiveStocks,
                negativeStocks
        );
    }

}
