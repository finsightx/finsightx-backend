package com.finsightx.finsightx_backend.repository;

import com.finsightx.finsightx_backend.domain.PolicySignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicySignalRepository extends JpaRepository<PolicySignal, Long> {

    List<PolicySignal> findByUserIdOrderByCreatedAtDesc(Long userId);

}
