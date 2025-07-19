package com.finsightx.finsightx_backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
public class PolicyInfoResponse {

    private Long policyId;

    private String policyName;

    private String stage;

    private OffsetDateTime createdAt;

    private String summary;

    private String content;

    private List<IndustryResponse> positiveIndustries;

    private List<IndustryResponse> negativeIndustries;

    private List<StockResponse> positiveStocks;

    private List<StockResponse> negativeStocks;

}
