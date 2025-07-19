package com.finsightx.finsightx_backend.dto.response;

import com.finsightx.finsightx_backend.domain.PolicyInfo;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
public class DailyReportResponse {

    private Long reportId;

    private String title;

    private OffsetDateTime createdAt;

    private List<PolicyInfoResponse> policies;

}
