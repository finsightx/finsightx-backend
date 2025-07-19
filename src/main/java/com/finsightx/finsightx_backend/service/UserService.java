package com.finsightx.finsightx_backend.service;

import com.finsightx.finsightx_backend.domain.PortfolioItem;
import com.finsightx.finsightx_backend.domain.Stock;
import com.finsightx.finsightx_backend.domain.User;
import com.finsightx.finsightx_backend.repository.StockRepository;
import com.finsightx.finsightx_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
        return userRepository.findById(userId)
                .map(User::getPortfolio)
                .orElseThrow(() -> new IllegalArgumentException("User ID " + userId + "를 찾을 수 없습니다."));
    }

    public List<PortfolioItem> findPortfolioStocksByIndustryCode(Long userId, String industryCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User ID " + userId + "를 찾을 수 없습니다."));

        List<PortfolioItem> userPortfolio = user.getPortfolio();
        if (userPortfolio == null || userPortfolio.isEmpty()) {
            return new ArrayList<>();
        }

        return userPortfolio.stream()
                .filter(item -> {
                    Optional<Stock> stockOptional = stockRepository.findByStockCode(item.getStockCode());
                    return stockOptional.map(stock -> stock.getIndustryCode().equals(industryCode)).orElse(false);
                })
                .collect(Collectors.toList());
    }

}


