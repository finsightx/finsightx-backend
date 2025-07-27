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

    @Query(value = "SELECT DISTINCT dr.* FROM daily_report dr " +
            "LEFT JOIN policy_info pi ON EXISTS (SELECT 1 FROM jsonb_array_elements_text(dr.policies) AS elem WHERE elem = pi.policy_id::text) " +
            "LEFT JOIN stock s ON " +
            "  (EXISTS (SELECT 1 FROM jsonb_array_elements_text(pi.positive_stocks) AS elem WHERE elem = s.stock_code) OR " +
            "   EXISTS (SELECT 1 FROM jsonb_array_elements_text(pi.negative_stocks) AS elem WHERE elem = s.stock_code) OR " +
            "   EXISTS (SELECT 1 FROM jsonb_array_elements_text(pi.positive_industries) AS elem WHERE elem = s.industry_code) OR " +
            "   EXISTS (SELECT 1 FROM jsonb_array_elements_text(pi.negative_industries) AS elem WHERE elem = s.industry_code)) " +
            "WHERE LOWER(dr.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "      LOWER(pi.policy_name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "      LOWER(pi.stage) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "      LOWER(pi.summary) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "      LOWER(CAST(pi.content AS text)) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "      LOWER(s.stock_name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "      LOWER(s.industry_name) LIKE LOWER(CONCAT('%', :keyword, '%'))",
            nativeQuery = true)
    List<DailyReport> searchDailyReports(@Param("keyword") String keyword);


}
