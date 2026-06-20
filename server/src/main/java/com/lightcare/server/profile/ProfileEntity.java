package com.lightcare.server.profile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 家庭档案。一个用户可以拥有 0..4 份档案（PRD §1.3 决策）。
 * 代填关系：profile.managedByUserId 非空时，表示该档案由该用户代填。
 */
@Entity
@Table(
    name = "lc_profile",
    indexes = {
        @Index(name = "idx_profile_owner", columnList = "ownerUserId"),
        @Index(name = "idx_profile_manager", columnList = "managedByUserId")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class ProfileEntity {

    public enum Relation { SELF, FAMILY_OTHER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 档案创建者（手机号注册人） */
    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    /** 代填人；为 null 表示自管 */
    @Column(name = "managed_by_user_id")
    private Long managedByUserId;

    @Column(length = 64, nullable = false)
    private String displayName;

    @Column(length = 512)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Relation relation = Relation.SELF;

    /** 生理数据（方案 3）：用于 Mifflin-St Jeor 公式算 BMR→TDEE→营养目标 */
    private LocalDate birthDate;

    @Column(length = 8)
    private String gender;  // M / F / U

    /** 身高 cm，可空（未采集时按年龄段默认） */
    @Column(name = "height_cm")
    private Integer heightCm;

    /** 体重 kg，可空 */
    @Column(name = "weight_kg")
    private Double weightKg;

    /** 活动量：SEDENTARY / LIGHT / MODERATE / ACTIVE / VERY_ACTIVE */
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level", length = 16)
    private ActivityLevel activityLevel = ActivityLevel.LIGHT;

    public enum ActivityLevel {
        SEDENTARY(1.2),     // 久坐
        LIGHT(1.375),        // 轻度（每周 1-3 次运动）
        MODERATE(1.55),      // 中度（每周 3-5 次）
        ACTIVE(1.725),       // 高度（每周 6-7 次）
        VERY_ACTIVE(1.9);    // 极高（运动员）
        public final double factor;
        ActivityLevel(double f) { this.factor = f; }
    }

    /** 默认按年龄段三档，P1.4 决策 */
    @Column(name = "protein_target_g", nullable = false)
    private int proteinTargetG = 60;
    @Column(name = "veg_target_servings", nullable = false)
    private int vegTargetServings = 5;
    @Column(name = "water_target_ml", nullable = false)
    private int waterTargetMl = 1700;
    @Column(name = "step_target", nullable = false)
    private int stepTarget = 8000;
    @Column(name = "calorie_target_kcal", nullable = false)
    private int calorieTargetKcal = 2000;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void touch() {
        this.updatedAt = Instant.now();
    }
}
