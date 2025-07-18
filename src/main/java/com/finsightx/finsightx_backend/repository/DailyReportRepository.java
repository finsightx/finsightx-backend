package com.finsightx.finsightx_backend.repository;

import com.finsightx.finsightx_backend.domain.DailyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DailyReportRepository extends JpaRepository<DailyReport, Integer> {

    List<DailyReport> findByOrderByCreatedAtDesc();

}
