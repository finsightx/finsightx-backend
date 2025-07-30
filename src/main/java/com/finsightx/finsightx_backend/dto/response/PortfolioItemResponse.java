package com.finsightx.finsightx_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioItemResponse {

    private String stockName;

    private Integer quantity;

    private String industryName;

}
