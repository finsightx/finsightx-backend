package com.finsightx.finsightx_backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PolicySignalResponse {

    private Long policySignalId;

    private String message;

    private PolicyInfoResponse policyInfo;

    private List<String> stockNames;

}
