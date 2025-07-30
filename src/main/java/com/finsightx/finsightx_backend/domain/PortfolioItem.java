package com.finsightx.finsightx_backend.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioItem {

    @JsonProperty("stock_code")
    private String stockCode;

    private int quantity;

}
