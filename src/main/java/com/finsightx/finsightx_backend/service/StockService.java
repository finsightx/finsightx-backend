package com.finsightx.finsightx_backend.service;

import com.finsightx.finsightx_backend.domain.Stock;
import com.finsightx.finsightx_backend.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    public List<Stock> findAll() {return stockRepository.findAll();}

    public List<Stock> getStocksByStockCodeIn(List<String> stockCodes) {
        return stockRepository.findByStockCodeIn(stockCodes);
    }

    public List<Stock> getStocksByIndustryCodeIn(List<String> industryCodes) {
        return stockRepository.findByIndustryCodeIn(industryCodes);
    }

}
