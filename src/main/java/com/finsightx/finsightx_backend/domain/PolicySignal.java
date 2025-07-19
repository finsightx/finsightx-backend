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

    @Column(name = "stock_names", columnDefinition = "jsonb", nullable = false)
    @Convert(converter = JsonbStringListConverter.class)
    private List<String> stockNames;
}