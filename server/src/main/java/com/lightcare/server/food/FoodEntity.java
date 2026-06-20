package com.lightcare.server.food;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * 食物库条目（PR-C）。
 *
 * 作用域：owner_user_id = 用户自己的食物；为 null 表示全局种子（所有用户只读共享，is_default=TRUE）。
 * 来源 source：MANUAL（手加）/ AI（拍照识别自动入库）/ SEED（V4 迁移的内置种子）。
 * 重名判定：display_name 相同 + 四项营养（kcal/protein/fat/carb）±5% 全等才算重复（应用层负责）。
 *   - 全等 → 跳过
 *   - 同名不同营养 → 写一条 conflict_status=PENDING_CONFLICT，等用户决定换名/覆盖/跳过
 */
@Entity
@Table(
    name = "lc_food_item",
    indexes = {
        @Index(name = "idx_food_owner_name", columnList = "ownerUserId,displayName"),
        @Index(name = "idx_food_owner_source", columnList = "ownerUserId,source"),
        @Index(name = "idx_food_conflict", columnList = "ownerUserId,conflictStatus")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class FoodEntity {

    public enum Source { MANUAL, AI, SEED }
    public enum ConflictStatus { RESOLVED, PENDING_CONFLICT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 归属用户；null = 全局种子 */
    @Column(name = "owner_user_id")
    private Long ownerUserId;

    /** 业务 key（"rice" / "custom_<ts>" / "ai_<ts>_<hash>"） */
    @Column(length = 64, nullable = false)
    private String key;

    @Column(name = "display_name", length = 128, nullable = false)
    private String displayName;

    @Column(length = 32, nullable = false)
    private String category = "其他";

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Source source = Source.MANUAL;

    /** 1 份的克数；AI 入库时 = M3 识别的 weightG */
    @Column(name = "per_serving_g")
    private int perServingG = 0;

    @Column(nullable = false)
    private int kcal = 0;

    @Column(name = "protein_g", nullable = false)
    private double proteinG = 0;

    @Column(name = "fat_g", nullable = false)
    private double fatG = 0;

    @Column(name = "carb_g", nullable = false)
    private double carbG = 0;

    @Column(name = "fiber_g", nullable = false)
    private double fiberG = 0;

    @Column(name = "water_ml", nullable = false)
    private int waterMl = 0;

    @Column(name = "veg_servings", nullable = false)
    private int vegServings = 0;

    /** TRUE = 内置种子，前端标识"不可删" */
    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "conflict_status", length = 16, nullable = false)
    private ConflictStatus conflictStatus = ConflictStatus.RESOLVED;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void touch() {
        this.updatedAt = Instant.now();
    }
}
