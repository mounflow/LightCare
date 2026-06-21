package com.lightcare.server.config;

import com.lightcare.server.auth.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 账号系统已上线：所有 /v1/** 默认要求 JWT，公开路径白名单放行。
 *
 * JwtAuthFilter 解析 Authorization: Bearer → 写 request attr "userId"；
 * CurrentUserResolver 从 attr 读 → 注入 @CurrentUserAnnotation long userId。
 *
 * 不引入 spring-security 的 Authentication（保持 stateless / 不进 SecurityContextHolder），
 * 因为家庭场景不需要角色，所有 controller 一视同仁只关心 userId。
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/v1/auth/**",          // register / login / me / logout
                    "/v1/hello",
                    "/v1/profiles/bootstrap", // 兼容老 client
                    "/v1/recognize"          // 公开识别
                ).permitAll()
                .anyRequest().permitAll()    // 鉴权由 JwtAuthFilter 强制（未带 token → 401 JSON）
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}