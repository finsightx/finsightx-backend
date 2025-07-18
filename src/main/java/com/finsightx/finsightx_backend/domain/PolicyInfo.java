package com.finsightx.finsightx_backend.domain;

import com.finsightx.finsightx_backend.converter.JsonbStringListConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

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

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "positive_industries", columnDefinition = "jsonb", nullable = false)
    @Convert(converter = JsonbStringListConverter.class)
    private List<String> positiveIndustries;

    @Column(name = "negative_industries", columnDefinition = "jsonb", nullable = false)
    @Convert(converter = JsonbStringListConverter.class)
    private List<String> negativeIndustries;

    @Column(name = "positive_stocks", columnDefinition = "jsonb", nullable = false)
    @Convert(converter = JsonbStringListConverter.class)
    private List<String> positiveStocks;

    @Column(name = "negative_stocks", columnDefinition = "jsonb", nullable = false)
    @Convert(converter = JsonbStringListConverter.class)
    private List<String> negativeStocks;
}
