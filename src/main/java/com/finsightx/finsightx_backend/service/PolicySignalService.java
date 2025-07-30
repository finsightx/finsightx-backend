package com.finsightx.finsightx_backend.service;


import com.finsightx.finsightx_backend.domain.PolicySignal;
import com.finsightx.finsightx_backend.dto.response.PolicyInfoResponse;
import com.finsightx.finsightx_backend.dto.response.PolicySignalListItemResponse;
import com.finsightx.finsightx_backend.dto.response.PolicySignalResponse;
import com.finsightx.finsightx_backend.repository.PolicySignalRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PolicySignalService {

    private final PolicySignalRepository policySignalRepository;
    private final PolicyInfoService policyInfoService;

    public Optional<PolicySignal> getPolicySignalById(Long policySignalId) {
        return policySignalRepository.findById(policySignalId);
    }

    public List<PolicySignal> getUserPolicySignals(Long userId) {
        return policySignalRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public PolicySignal createPolicySignal(Long userId, String message, Long policyId, List<String> stockNames, OffsetDateTime date) {
        PolicySignal policySignal = new PolicySignal();
        policySignal.setUserId(userId);
        policySignal.setMessage(message);
        policySignal.setPolicyId(policyId);
        policySignal.setCreatedAt(date);
        policySignal.setIsRead(false);
        policySignal.setStockNames(stockNames);

        return policySignalRepository.save(policySignal);
    }

    @Transactional
    public PolicySignal markPolicySignalAsRead(Long policySignalId) {
        PolicySignal policySignal = getPolicySignalById(policySignalId)
                .orElseThrow(() -> new IllegalArgumentException("Policy Signal ID " + policySignalId + "를 찾을 수 없습니다."));
        policySignal.setIsRead(true);
        return policySignalRepository.save(policySignal);
    }

    public List<PolicySignalListItemResponse> getUserPolicySignalsAsDto(Long userId) {
        List<PolicySignal> signals = getUserPolicySignals(userId);

        if (signals.isEmpty()) return Collections.emptyList();

        return signals.stream()
                .map(signal -> new PolicySignalListItemResponse(
                        signal.getPolicySignalId(),
                        signal.getMessage(),
                        signal.getPolicyId(),
                        signal.getCreatedAt().atZoneSameInstant(ZoneId.of("Asia/Seoul")).toOffsetDateTime(),
                        signal.getIsRead(),
                        signal.getStockNames()
                ))
                .collect(Collectors.toList());
    }

    public Optional<PolicySignalResponse> getPolicySignalAsDto(Long policySignalId) {
        return getPolicySignalById(policySignalId)
                .map(signal -> {
                    PolicyInfoResponse policyInfoResponse = null;
                    if (signal.getPolicyId() != null) {
                        policyInfoResponse = policyInfoService.getPolicyInfoById(signal.getPolicyId())
                                .map(policyInfoService::toPolicyInfoResponse)
                                .orElse(null);
                    }
                    return new PolicySignalResponse(
                            signal.getPolicySignalId(),
                            signal.getMessage(),
                            policyInfoResponse,
                            signal.getStockNames()
                    );
                });
    }

    @Transactional
    public PolicySignalListItemResponse markPolicySignalAsReadAndGetDto(Long policySignalId) {
        PolicySignal updatedSignal = markPolicySignalAsRead(policySignalId);
        return new PolicySignalListItemResponse(
                updatedSignal.getPolicySignalId(),
                updatedSignal.getMessage(),
                updatedSignal.getPolicyId(),
                updatedSignal.getCreatedAt().atZoneSameInstant(ZoneId.of("Asia/Seoul")).toOffsetDateTime(),
                updatedSignal.getIsRead(),
                updatedSignal.getStockNames()
        );
    }

}
