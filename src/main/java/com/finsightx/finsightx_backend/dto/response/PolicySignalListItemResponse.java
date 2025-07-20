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
public class PolicySignalListItemResponse {

    private Long policySignalId;

    private String message;

    private Long policyId;

    private OffsetDateTime createdAt;

    private Boolean isRead;

    private List<String> stockNames;

}
