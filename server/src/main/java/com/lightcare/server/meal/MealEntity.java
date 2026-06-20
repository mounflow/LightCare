package com.lightcare.server.meal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 一次饮食记录。一餐可能含 N 个食材，先按"餐别一行"简化实现。
 */
@Entity
@Table(
    name = "lc_meal",
    indexes = {
        @Index(name = "idx_meal_profile_date", columnList = "profileId,mealDate"),
        @Index(name = "idx_meal_profile_time", columnList = "profileId,loggedAt")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class MealEntity {

    public enum Slot { BREAKFAST, LUNCH, DINNER, SNACK }
    public enum Portion { SMALL, MEDIUM, LARGE }
    public enum Source { PHOTO, VOICE, SEARCH, MANUAL }
    /** 异步识别状态：拍照即入列后服务端异步调 M3 回写营养字段。DONE=已识别（含手动），PENDING=识别中，FAILED=识别失败（保留用户填值）。 */
    public enum RecognitionStatus { DONE, PENDING, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Slot slot;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Portion portion = Portion.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Source source = Source.MANUAL;

    /** 食材名称简短描述（"番茄炒蛋 + 米饭"），或拍照 AI 原始 OCR 文本 */
    @Column(length = 1024, nullable = false)
    private String summary;

    /** 估算营养（先按份量 + 默认食材库规则；P4 收尾接 AI） */
    // 显式 name：Spring Boot 3.3 默认命名策略对 carbG/fatG 推导有 bug（缺下划线）。
    @Column(name = "kcal", nullable = false) private int kcal = 0;
    @Column(name = "protein_g", nullable = false) private double proteinG = 0;
    @Column(name = "fat_g", nullable = false) private double fatG = 0;
    @Column(name = "carb_g", nullable = false) private double carbG = 0;
    @Column(name = "fiber_g", nullable = false) private double fiberG = 0;
    @Column(name = "veg_servings", nullable = false) private int vegServings = 0;

    /** 本餐含水量（毫升），用于 HomeScreen 余量第 4 项「水分」。 */
    @Column(name = "water_ml", nullable = false) private int waterMl = 0;

    /** 异步识别状态。手动/食物库历史 = DONE；拍照走异步识别先 PENDING 后回写 DONE/FAILED。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "recognition_status", length = 16, nullable = false)
    private RecognitionStatus recognitionStatus = RecognitionStatus.DONE;

    /** 地点名（反地理编码，如「北京市海淀区」/「妈妈家」），可空。 */
    @Column(name = "location", length = 128)
    private String location;

    /** 关于这次美食的描述/笔记（用户手填心情），可空；与 summary（自动拼的食物名）分开。 */
    @Column(name = "description", length = 512)
    private String description;

    /**
     * 一餐的食材明细 JSON（按食物分类染色用）。
     * 形如 [{"name":"鸡胸","category":"蛋白","weightG":150,"kcal":165,"proteinG":31,"fatG":3.6,"carbG":0,"waterMl":0}, ...]
     * 由 App 端在创建时一并写入；识别完成时 M3 也可补写。
     * 旧数据为空串 → 染色按 slot 兜底。
     */
    @Column(name = "items_json", columnDefinition = "TEXT")
    private String itemsJson = "";

    @Column(name = "meal_date", nullable = false)
    private LocalDate mealDate;

    @Column(name = "meal_time", nullable = false)
    private LocalTime mealTime;

    @Column(name = "logged_at", nullable = false)
    private Instant loggedAt = Instant.now();
}
