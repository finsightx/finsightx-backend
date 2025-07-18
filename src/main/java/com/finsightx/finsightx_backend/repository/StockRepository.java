package com.finsightx.finsightx_backend.repository;

import com.finsightx.finsightx_backend.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockRepository extends JpaRepository<Stock, Integer> {
}
