package com.finsightx.finsightx_backend.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioItem {
    private String stockCode;
    private int quantity;
}
