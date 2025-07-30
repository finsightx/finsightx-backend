package com.finsightx.finsightx_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PolicySignalResponse {

    private Long policySignalId;

    private String message;

    private PolicyInfoResponse policyInfo;

    private List<String> stockNames;

}
