package com.lightcare.server.common;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Controller 方法签名中 @CurrentUser long userId 可自动注入。
 * P3 阶段先做占位：从 Header "X-LightCare-User-Id" 读取。
 * P3 收尾会切到 JWT 解析。
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
        String header = webRequest.getHeader("X-LightCare-User-Id");
        if (header == null || header.isBlank()) {
            throw new ApiException(ApiError.UNAUTHORIZED);
        }
        try {
            return Long.parseLong(header.trim());
        } catch (NumberFormatException e) {
            throw new ApiException(ApiError.UNAUTHORIZED, "无效的登录态");
        }
    }
}
