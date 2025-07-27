package com.finsightx.finsightx_backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "policy_info")
public class PolicyInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long policyId;

    @Column(name = "policy_name", nullable = false)
    private String policyName;

    @Column(name = "stage", nullable = false)
    private String stage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "summary", nullable = false)
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", columnDefinition = "jsonb", nullable = false)
    private List<String> content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "positive_industries", columnDefinition = "jsonb", nullable = false)
    private List<String> positiveIndustries;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "negative_industries", columnDefinition = "jsonb", nullable = false)
    private List<String> negativeIndustries;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "positive_stocks", columnDefinition = "jsonb", nullable = false)
    private List<String> positiveStocks;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "negative_stocks", columnDefinition = "jsonb", nullable = false)
    private List<String> negativeStocks;
}
