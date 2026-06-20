package com.lightcare.server.exercise;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/** 喝水打卡：每次点 +1 杯（200ml） */
@Entity
@Table(
    name = "lc_water_log",
    indexes = { @Index(name = "idx_water_profile_date", columnList = "profileId,logDate") }
)
@Getter
@Setter
@NoArgsConstructor
public class WaterLogEntity {

    public static final int CUP_ML = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(nullable = false)
    private int cups = 1;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "logged_at", nullable = false)
    private Instant loggedAt = Instant.now();
}
