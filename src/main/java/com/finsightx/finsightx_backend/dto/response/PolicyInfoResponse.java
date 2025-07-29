package com.finsightx.finsightx_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PolicyInfoResponse {

    private Long policyId;

    private String policyName;

    private String stage;

    private OffsetDateTime createdAt;

    private String summary;

    private List<String> content;

    private List<IndustryResponse> positiveIndustries;

    private List<IndustryResponse> negativeIndustries;

    private List<StockResponse> positiveStocks;

    private List<StockResponse> negativeStocks;

    private String originalUrl;

}
