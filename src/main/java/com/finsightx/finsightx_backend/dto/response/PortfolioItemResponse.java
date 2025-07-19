package com.finsightx.finsightx_backend.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PortfolioItemResponse {

    private String stockName;

    private Integer quantity;

    private String industryName;

}
