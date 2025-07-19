package com.finsightx.finsightx_backend.service;

import com.finsightx.finsightx_backend.domain.Stock;
import com.finsightx.finsightx_backend.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    public Optional<Stock> getStockByStockCode(String stockCode) {
        return stockRepository.findByStockCode(stockCode);
    }

    public List<Stock> getStocksByIndustryCode(String industryCode) {
        return stockRepository.findByIndustryCode(industryCode);
    }

}
