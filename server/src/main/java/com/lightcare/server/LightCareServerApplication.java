package com.lightcare.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class LightCareServerApplication {

    public static void main(String[] args) {
        // 固定 JVM 默认时区为上海：LocalDate.now()/LocalTime.now()（建 meal 时设 meal_date/meal_time）
        // 依赖 JVM 默认时区，不显式设的话依赖部署机 TZ 环境变量，凌晨可能跨天导致首页今日记录查不到。
        // 必须在 SpringApplication.run 之前设。
        System.setProperty("user.timezone", "Asia/Shanghai");
        SpringApplication.run(LightCareServerApplication.class, args);
    }

    /**
     * Spring Boot 3.3 默认的 SpringPhysicalNamingStrategy 对"驼峰后接单个大写字母"字段
     * （如 carbG / fatG / proteinTargetG）推导为 carbg / fatg / protein_targetg，
     * 与 Flyway 建表时的 carb_g / fat_g / protein_target_g 不一致，导致 schema 校验或
     * 运行时 SQL 失败。显式切到 CamelCaseToUnderscoresNamingStrategy。
     */
    @Bean
    public HibernatePropertiesCustomizer hibernateNamingCustomizer() {
        return props -> props.put("hibernate.physical_naming_strategy",
            "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
    }
}
