package com.finsightx.finsightx_backend.domain;

import com.finsightx.finsightx_backend.converter.JsonbPortfolioListConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(name = "portfolio", columnDefinition = "jsonb")
    @Convert(converter = JsonbPortfolioListConverter.class)
    private List<PortfolioItem> portfolio;

}