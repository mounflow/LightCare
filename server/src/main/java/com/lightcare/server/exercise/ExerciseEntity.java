package com.lightcare.server.exercise;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
    name = "lc_exercise",
    indexes = { @Index(name = "idx_ex_profile_date", columnList = "profileId,exerciseDate") }
)
@Getter
@Setter
@NoArgsConstructor
public class ExerciseEntity {

    public enum Kind { WALK, BRISK_WALK, JOG, STRETCH, OTHER }
    public enum Intensity { VERY_LIGHT, LIGHT, MODERATE, MODERATE_HIGH }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Kind kind;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Intensity intensity = Intensity.LIGHT;

    @Column(name = "duration_min", nullable = false)
    private int durationMin;

    @Column(nullable = false)
    private int steps;

    /** 用户自报疲劳度 0..3（P5 §5.2 决策表） */
    @Column(nullable = false)
    private int fatigue = 1;

    @Column(name = "exercise_date", nullable = false)
    private LocalDate exerciseDate;

    @Column(name = "logged_at", nullable = false)
    private Instant loggedAt = Instant.now();
}
