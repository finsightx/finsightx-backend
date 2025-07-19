package com.finsightx.finsightx_backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
public class DailyReportListItemResponse {

    private Long reportId;

    private String title;

    private OffsetDateTime createdAt;

    private List<String> industries;

    private List<String> stocks;

}
