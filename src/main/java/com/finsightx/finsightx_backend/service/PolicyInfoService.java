package com.finsightx.finsightx_backend.service;

import com.finsightx.finsightx_backend.domain.PolicyInfo;
import com.finsightx.finsightx_backend.repository.PolicyInfoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PolicyInfoService {

    private final PolicyInfoRepository policyInfoRepository;

    public Optional<PolicyInfo> getPolicyInfoById(Long policyId) {
        return policyInfoRepository.findById(policyId);
    }

    // TODO: Check
    @Transactional
    public PolicyInfo savePolicyInfo(PolicyInfo policyInfo) {
        if (policyInfo.getCreatedAt() == null) {
            policyInfo.setCreatedAt(OffsetDateTime.now());
        }
        return policyInfoRepository.save(policyInfo);
    }

}
