package com.lightcare.server.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lightcare.server.common.ApiError;
import com.lightcare.server.common.ApiResponse;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 鉴权 Filter。
 *
 * - 公开路径（白名单）直通：`/v1/auth/**`、`/v1/hello`、`/v1/profiles/bootstrap`、`/v1/recognize`
 * - 其它路径：从 `Authorization: Bearer <token>` 解析 userId → 写 `request.setAttribute("userId", uid)`
 *   - 解析失败 → 写 `ApiResponse.fail(UNAUTHORIZED)` JSON + HTTP 401（**不走 GlobalExceptionHandler**，
 *     因为 Filter 阶段早于 DispatcherServlet）
 * - 兼容 fallback：若 `X-LightCare-User-Id` header 存在（老 client 没升级），也接受；不推荐长期保留
 *
 * 依赖：[CurrentUserResolver] 改成读 `request.getAttribute("userId")`。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    private static final List<String> PUBLIC_PATHS = List.of(
        "/v1/auth/register",
        "/v1/auth/login",
        "/v1/auth/logout",
        "/v1/hello",
        "/v1/profiles/bootstrap",
        "/v1/recognize"
    );

    private static final String USER_ID_ATTR = "userId";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // OPTIONS 永远直通（CORS 预检）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String path = request.getRequestURI();
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String auth = request.getHeader("Authorization");
        Long uid = null;

        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring("Bearer ".length()).trim();
            if (!token.isEmpty()) {
                try {
                    uid = jwtUtil.parse(token);
                } catch (JwtException e) {
                    log.debug("JWT parse failed: {}", e.getMessage());
                    writeUnauthorized(response, "登录已过期或无效，请重新登录");
                    return;
                }
            }
        }

        // 兼容 fallback：老 client 还在用 X-LightCare-User-Id 头（dev 期允许）
        if (uid == null) {
            String legacy = request.getHeader("X-LightCare-User-Id");
            if (legacy != null && !legacy.isBlank()) {
                try { uid = Long.parseLong(legacy.trim()); }
                catch (NumberFormatException ignore) { /* 忽略，走 401 */ }
            }
        }

        if (uid == null) {
            writeUnauthorized(response, "请先登录");
            return;
        }

        try {
            request.setAttribute(USER_ID_ATTR, uid);
            chain.doFilter(request, response);
        } finally {
            request.removeAttribute(USER_ID_ATTR);
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<Void> body = new ApiResponse<>(ApiError.UNAUTHORIZED.code, message, null);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}