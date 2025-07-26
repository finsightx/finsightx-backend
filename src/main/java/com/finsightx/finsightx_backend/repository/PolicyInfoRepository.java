package com.finsightx.finsightx_backend.repository;

import com.finsightx.finsightx_backend.domain.PolicyInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface PolicyInfoRepository extends JpaRepository<PolicyInfo, Long> {

    List<PolicyInfo> findByPolicyIdIn(List<Long> ids);

    List<PolicyInfo> findByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);
}
