package com.lightcare.server.food;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * 食物做法（PR-Recipe）。
 *
 * 一对一挂 lc_food_item：一份 recipe 描述一个 food 的"做法 / 食材 / 调料 / 烹饪时间 / 难度"。
 * 数据源：
 *   - 用户手填（在 AddFoodDialog 二级编辑器里填）→ source=MANUAL
 *   - 拍照识别时 M3 顺便输出 → source=AI（参考 MealRecognitionExecutor 集成）
 *
 * 食材/调料/步骤用 JSONB 存：避免关联表，且一次 SELECT 拿全。
 * Hibernate 用 @JdbcTypeCode(SqlTypes.JSON) 映射 PostgreSQL JSONB。
 *
 * ⚠️ 驼峰字段（cookingMinutes / ingredientsJson 等）必须显式 @Column(name="snake_case")，
 * 否则 Hibernate 6.5 默认命名策略会推成 cookingminutes（缺下划线），启动时
 * "column does not exist" 失败。
 */
@Entity
@Table(name = "lc_food_recipe")
@Getter
@Setter
@NoArgsConstructor
public class RecipeEntity {

    public enum Source { MANUAL, AI }

    @Id
    @Column(name = "food_id")
    private Long foodId;

    @Column(name = "cooking_minutes", nullable = false)
    private int cookingMinutes = 0;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Difficulty difficulty = Difficulty.EASY;

    public enum Difficulty { EASY, MEDIUM, HARD }

    /** 食材列表：JSON 字符串（前端：List<Ingredient>）。空 = "[]"。 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ingredients_json", columnDefinition = "jsonb", nullable = false)
    private String ingredientsJson = "[]";

    /** 调料列表：JSON 字符串。空 = "[]"。 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "seasonings_json", columnDefinition = "jsonb", nullable = false)
    private String seasoningsJson = "[]";

    /** 步骤列表：JSON 字符串。空 = "[]"。 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "steps_json", columnDefinition = "jsonb", nullable = false)
    private String stepsJson = "[]";

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Source source = Source.MANUAL;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void touch() {
        this.updatedAt = Instant.now();
    }
}
