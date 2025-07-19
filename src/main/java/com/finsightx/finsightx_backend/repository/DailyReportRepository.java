package com.finsightx.finsightx_backend.repository;

import com.finsightx.finsightx_backend.domain.DailyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DailyReportRepository extends JpaRepository<DailyReport, Long> {

    List<DailyReport> findByOrderByCreatedAtDesc();

    @Query(value = "SELECT DISTINCT dr FROM DailyReport dr " +
            "LEFT JOIN PolicyInfo pi ON EXISTS (SELECT 1 FROM jsonb_array_elements_text(dr.policies) AS elem WHERE elem = pi.policy_id::text) " +
            "LEFT JOIN Stock s ON " +
            "  (jsonb_exists_any(pi.positiveStocks, s.stockCode) = true OR " +
            "   jsonb_exists_any(pi.negativeStocks, s.stockCode) = true OR " +
            "   jsonb_exists_any(pi.positiveIndustries, s.industryCode) = true OR " +
            "   jsonb_exists_any(pi.negativeIndustries, s.industryCode) = true) " +
            "WHERE LOWER(dr.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "      LOWER(pi.policyName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "      LOWER(pi.stage) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "      LOWER(pi.summary) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "      LOWER(pi.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "      LOWER(s.stockName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "      LOWER(s.industryName) LIKE LOWER(CONCAT('%', :keyword, '%'))",
        nativeQuery = true)
    List<DailyReport> searchDailyReports(@Param("keyword") String keyword);


}
