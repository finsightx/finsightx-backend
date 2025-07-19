package com.finsightx.finsightx_backend.service;


import com.finsightx.finsightx_backend.domain.PolicySignal;
import com.finsightx.finsightx_backend.repository.PolicySignalRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PolicySignalService {

    private final PolicySignalRepository policySignalRepository;

    public Optional<PolicySignal> getPolicySignalById(Long policySignalId) {
        return policySignalRepository.findById(policySignalId);
    }

    public List<PolicySignal> getUserPolicySignals(Long userId) {
        return policySignalRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // TODO: Check
    @Transactional
    public PolicySignal createPolicySignal(Long userId, Long policyId, String message, List<String> stockNames) {
        PolicySignal policySignal = new PolicySignal();
        policySignal.setUserId(userId);
        policySignal.setPolicyId(policyId);
        policySignal.setMessage(message);
        policySignal.setCreatedAt(OffsetDateTime.now());
        policySignal.setIsRead(false);
        policySignal.setStockNames(stockNames);

        return policySignalRepository.save(policySignal);
    }

    @Transactional
    public PolicySignal markPolicySignalAsRead(Long policySignalId) {
        PolicySignal policySignal = policySignalRepository.findById(policySignalId)
                .orElseThrow(() -> new IllegalArgumentException("Policy Signal ID " + policySignalId + "를 찾을 수 없습니다."));
        policySignal.setIsRead(true);
        return policySignalRepository.save(policySignal);
    }

}
