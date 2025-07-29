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
public class ClovaResponse {

    private Status status;

    private Result result;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Status {
        private String code;
        private String message;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Result {
        private Long created;
        private Usage usage;
        private ClovaMessage message;
        private Long seed;
        private List<AiFilter> aiFilter;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        private int completionTokens;
        private int promptTokens;
        private int totalTokens;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiFilter {
        private String groupName;
        private String name;
        private String score;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PolicyInfoFromLlm {
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

}
