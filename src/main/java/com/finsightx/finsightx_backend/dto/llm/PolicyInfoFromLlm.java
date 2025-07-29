package com.finsightx.finsightx_backend.dto.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PolicyInfoFromLlm {

    @JsonProperty("isPolicyChange")
    private boolean isPolicyChange;

    private String policyName;

    private String stage;

    private String summary;

    private List<String> content;

    private List<String> positiveIndustries;

    private List<String> negativeIndustries;

    private List<String> positiveStocks;

    private List<String> negativeStocks;

}