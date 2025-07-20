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
public class DailyReportListItemResponse {

    private Long reportId;

    private String title;

    private OffsetDateTime createdAt;

    private List<String> industries;

    private List<String> stocks;

}
