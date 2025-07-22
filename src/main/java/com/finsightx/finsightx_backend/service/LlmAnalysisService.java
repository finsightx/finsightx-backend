package com.finsightx.finsightx_backend.service;

import com.finsightx.finsightx_backend.domain.PolicyInfo;
import com.finsightx.finsightx_backend.dto.llm.LlmPolicyAnalysisRequest;
import com.finsightx.finsightx_backend.dto.llm.LlmPolicyAnalysisResponse;
import com.finsightx.finsightx_backend.dto.policyNewsApi.PolicyNewsItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmAnalysisService {

    private final RestTemplate restTemplate;
// TODO
//    @Value("${llm.api.endpoint}")
    private String llmApiEndpoint;

    public PolicyInfo analyzePolicyNewsWithLlm(PolicyNewsItem newsItem) {
        LlmPolicyAnalysisRequest request = new LlmPolicyAnalysisRequest(
                newsItem.getTitle(),
                newsItem.getSubTitle1(),
                newsItem.getDataContents()
        );

        try {
            // TODO: 임시
            LlmPolicyAnalysisResponse llmResponse = restTemplate.postForObject(llmApiEndpoint, request, LlmPolicyAnalysisResponse.class);

            if (llmResponse != null && llmResponse.isPolicyChange()) {
                log.info("LLM determined as policy change news: {}", llmResponse.getPolicyName());
                return convertToPolicyInfo(llmResponse);
            } else {
                log.info("LLM determined it's general news or unsuitable for PolicyInfo processing. News Title: {}", newsItem.getTitle());
                return null;
            }
        } catch (Exception e) {
            log.error("Error during LLM analysis. News Title: {}", newsItem.getTitle(), e);
            return null;
        }
    }

    private PolicyInfo convertToPolicyInfo(LlmPolicyAnalysisResponse llmResponse) {
        PolicyInfo policyInfo = new PolicyInfo();
        policyInfo.setPolicyName(llmResponse.getPolicyName());
        policyInfo.setStage(llmResponse.getStage());
        policyInfo.setCreatedAt(OffsetDateTime.now(ZoneId.of("Asia/Seoul")));
        policyInfo.setSummary(llmResponse.getSummary());
        policyInfo.setContent(llmResponse.getContent());
        policyInfo.setPositiveIndustries(Optional.ofNullable(llmResponse.getPositiveIndustries()).orElse(Collections.emptyList()));
        policyInfo.setNegativeIndustries(Optional.ofNullable(llmResponse.getNegativeIndustries()).orElse(Collections.emptyList()));
        policyInfo.setPositiveStocks(Optional.ofNullable(llmResponse.getPositiveStocks()).orElse(Collections.emptyList()));
        policyInfo.setNegativeStocks(Optional.ofNullable(llmResponse.getNegativeStocks()).orElse(Collections.emptyList()));

        return policyInfo;
    }
}
