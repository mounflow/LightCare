package com.lightcare.server.profile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "lc_user")
@Getter
@Setter
@NoArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 32, unique = true)
    private String phone;

    @Column(name = "wechat_open_id", length = 64, unique = true)
    private String wechatOpenId;

    /** 账号系统：bcrypt 哈希（~60 字符，128 足够）；null = 老 bootstrap 创建的占位 user，未走 register */
    @Column(name = "password_hash", length = 128)
    private String passwordHash;

    @Column(length = 64, nullable = false)
    private String nickname;

    @Column(length = 512)
    private String avatarUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void touch() {
        this.updatedAt = Instant.now();
    }
}
