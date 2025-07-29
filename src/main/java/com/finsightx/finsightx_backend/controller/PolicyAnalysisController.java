package com.finsightx.finsightx_backend.controller;

import com.finsightx.finsightx_backend.dto.request.NewsItemRequest;
import com.finsightx.finsightx_backend.dto.response.PolicyInfoResponse;
import com.finsightx.finsightx_backend.service.PolicyNewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class PolicyAnalysisController {

    private final PolicyNewsService policyNewsService;

    @PostMapping("/item")
    public ResponseEntity<PolicyInfoResponse> analysisItem(@RequestBody NewsItemRequest newsItem) {
        PolicyInfoResponse response = policyNewsService.analysisPolicyNewsItem(newsItem);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/date/{date}")
    public ResponseEntity<Void> processPolicyNewsByDate(@PathVariable String date) {
        policyNewsService.processPolicyNewsByDate(date);
        return ResponseEntity.ok().build();
    }

}
