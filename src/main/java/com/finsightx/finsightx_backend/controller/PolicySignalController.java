package com.finsightx.finsightx_backend.controller;

import com.finsightx.finsightx_backend.dto.response.PolicySignalListItemResponse;
import com.finsightx.finsightx_backend.dto.response.PolicySignalResponse;
import com.finsightx.finsightx_backend.service.PolicySignalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policy-signal")
@RequiredArgsConstructor
public class PolicySignalController {

    private final PolicySignalService policySignalService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PolicySignalListItemResponse>> getUserPolicySignals(@PathVariable Long userId) {
        List<PolicySignalListItemResponse> responses = policySignalService.getUserPolicySignalsAsDto(userId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{policySignalId}")
    public ResponseEntity<PolicySignalResponse> getPolicySignalById(@PathVariable Long policySignalId) {
        return policySignalService.getPolicySignalAsDto(policySignalId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{policySignalId}/read")
    public ResponseEntity<PolicySignalListItemResponse> markPolicySignalAsRead(@PathVariable Long policySignalId) {
        try {
            PolicySignalListItemResponse updatedDto = policySignalService.markPolicySignalAsReadAndGetDto(policySignalId);
            return ResponseEntity.ok(updatedDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

}
