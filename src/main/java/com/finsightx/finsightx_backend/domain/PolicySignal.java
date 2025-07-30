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
@Table(name = "policy_signal")
public class PolicySignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_signal_id")
    private Long policySignalId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "policy_id", nullable = false)
    private Long policyId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "stock_names", columnDefinition = "jsonb", nullable = false)
    private List<String> stockNames;
}