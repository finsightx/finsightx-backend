package com.finsightx.finsightx_backend.dto.llm;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LlmPolicyAnalysisResponse {

    private boolean isPolicyChange;

    private String policyName;

    private String stage;

    private String summary;

    private List<String> content;

    // List<String>: 업종/종목 코드 리스트
    private List<String> positiveIndustries;

    private List<String> negativeIndustries;

    private List<String> positiveStocks;

    private List<String> negativeStocks;

}
