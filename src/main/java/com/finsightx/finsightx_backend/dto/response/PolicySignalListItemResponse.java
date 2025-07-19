package com.finsightx.finsightx_backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
public class PolicySignalListItemResponse {

    private Long policySignalId;

    private String message;

    private Long policyId;

    private OffsetDateTime createdAt;

    private Boolean isRead;

    private List<String> stockNames;

}
