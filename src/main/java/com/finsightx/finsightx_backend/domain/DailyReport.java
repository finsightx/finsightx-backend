package com.finsightx.finsightx_backend.domain;

import com.finsightx.finsightx_backend.converter.JsonbLongListConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "daily_report")
public class DailyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "policies", columnDefinition = "jsonb", nullable = false)
    @Convert(converter = JsonbLongListConverter.class)
    private List<Long> policies;
}
