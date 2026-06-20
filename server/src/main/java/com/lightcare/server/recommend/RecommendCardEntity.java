package com.lightcare.server.recommend;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * 推荐卡。P5 收尾生成；P3 先建表与占位接口。
 */
@Entity
@Table(
    name = "lc_recommend_card",
    indexes = { @Index(name = "idx_recommend_profile", columnList = "profileId,createdAt") }
)
@Getter
@Setter
@NoArgsConstructor
public class RecommendCardEntity {

    public enum Kind { MEAL, EXERCISE }
    public enum Status { PENDING, ACCEPTED, SKIPPED, SWAPPED }

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
    private Status status = Status.PENDING;

    @Column(length = 1024, nullable = false)
    private String title;

    @Column(length = 1024, nullable = false)
    private String body;

    /** 候选菜谱 / 运动 id 列表，JSON 字符串 */
    @Column(length = 2048)
    private String candidatesJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "acted_at")
    private Instant actedAt;
}
