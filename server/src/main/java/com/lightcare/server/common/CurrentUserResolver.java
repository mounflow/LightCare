package com.lightcare.server.common;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Controller 方法签名中 `@CurrentUserAnnotation long userId` 自动注入。
 *
 * 来源优先级：
 *   1. request attr "userId"（由 JwtAuthFilter 写入，主流路径）
 *   2. Header "X-LightCare-User-Id"（兼容老 client / dev 期调试）
 *
 * 两个都没有 → UNAUTHORIZED。GlobalExceptionHandler 会把 UNAUTHORIZED 转 HTTP 401。
 */
@Component
public class CurrentUserResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserAnnotation.class)
            && (parameter.getParameterType() == long.class || parameter.getParameterType() == Long.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        // 1) JWT 已写入 attr（JwtAuthFilter 写）
        Object attr = webRequest.getAttribute("userId", NativeWebRequest.SCOPE_REQUEST);
        if (attr instanceof Long uid) return uid;

        // 2) fallback：老 header（dev / 兼容）
        String header = webRequest.getHeader("X-LightCare-User-Id");
        if (header != null && !header.isBlank()) {
            try { return Long.parseLong(header.trim()); }
            catch (NumberFormatException ignore) { /* fall through */ }
        }
        throw new ApiException(ApiError.UNAUTHORIZED);
    }
}