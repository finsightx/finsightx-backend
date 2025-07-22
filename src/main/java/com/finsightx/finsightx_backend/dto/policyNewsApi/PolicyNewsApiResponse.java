package com.finsightx.finsightx_backend.dto.policyNewsApi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PolicyNewsApiResponse {

    private String resultCode;

    private String resultMsg;

    private List<PolicyNewsItem> newsItems;
}
