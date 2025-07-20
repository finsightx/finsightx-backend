package com.finsightx.finsightx_backend.service;

import com.finsightx.finsightx_backend.domain.PortfolioItem;
import com.finsightx.finsightx_backend.domain.Stock;
import com.finsightx.finsightx_backend.domain.User;
import com.finsightx.finsightx_backend.dto.response.PortfolioItemResponse;
import com.finsightx.finsightx_backend.repository.StockRepository;
import com.finsightx.finsightx_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final StockRepository stockRepository;

    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    public List<PortfolioItem> getUserPortfolio(Long userId) {
        return getUserById(userId)
                .map(User::getPortfolio)
                .orElseThrow(() -> new IllegalArgumentException("User ID " + userId + "를 찾을 수 없습니다."));
    }

    public List<PortfolioItem> findPortfolioStocksByIndustryCode(Long userId, String industryCode) {
        User user = getUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User ID " + userId + "를 찾을 수 없습니다."));

        List<PortfolioItem> userPortfolio = user.getPortfolio();
        if (userPortfolio == null || userPortfolio.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> portfolioStockCodes = userPortfolio.stream()
                .map(PortfolioItem::getStockCode)
                .distinct()
                .collect(Collectors.toList());

        Map<String, Stock> stockMap = stockRepository.findByStockCodeIn(portfolioStockCodes).stream()
                .collect(Collectors.toMap(Stock::getStockCode, stock -> stock));

        return userPortfolio.stream()
                .filter(item -> {
                    Stock stock = stockMap.get(item.getStockCode());
                    return stock != null && stock.getIndustryCode().equals(industryCode);
                })
                .collect(Collectors.toList());
    }

    public List<PortfolioItemResponse> getMyAssetsAsDto(Long userId) {
        List<PortfolioItem> rawPortfolio = getUserPortfolio(userId);
        if (rawPortfolio.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> stockCodes = rawPortfolio.stream()
                .map(PortfolioItem::getStockCode)
                .distinct()
                .toList();

        Map<String, Stock> stockMap = stockRepository.findByStockCodeIn(stockCodes).stream()
                .collect(Collectors.toMap(Stock::getStockCode, stock -> stock));

        return rawPortfolio.stream()
                .map(item -> {
                    Stock stock = stockMap.get(item.getStockCode());
                    if (stock != null) {
                        return new PortfolioItemResponse(
                                stock.getStockName(),
                                item.getQuantity(),
                                stock.getIndustryName()
                        );
                    } else {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

}


