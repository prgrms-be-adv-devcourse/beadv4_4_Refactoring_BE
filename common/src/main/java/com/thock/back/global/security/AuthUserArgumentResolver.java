package com.thock.back.global.security;

import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.shared.member.domain.MemberRole;
import com.thock.back.shared.member.domain.MemberState;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @AuthUser 애노테이션이 붙은 AuthenticatedUser 파라미터를 자동으로 주입하는 Resolver
 * Gateway에서 전달한 X-Member-Id, X-Member-Role, X-Member-State 헤더를 읽어서 객체 생성
 */
@Slf4j
@Component
public class AuthUserArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String HEADER_MEMBER_ID = "X-Member-Id";
    private static final String HEADER_ROLE = "X-Member-Role";
    private static final String HEADER_STATE = "X-Member-State";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthUser.class) &&
               parameter.getParameterType().equals(AuthenticatedUser.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();

        String memberIdStr = request.getHeader(HEADER_MEMBER_ID);
        String roleStr = request.getHeader(HEADER_ROLE);
        String stateStr = request.getHeader(HEADER_STATE);

        // 인증 헤더 필수 검증
        if (memberIdStr == null || roleStr == null || stateStr == null) {
            log.warn("Missing auth headers. memberId={}, role={}, state={}", memberIdStr, roleStr, stateStr);
            throw new CustomException(ErrorCode.AUTH_CONTEXT_NOT_FOUND);
        }

        try {
            Long memberId = Long.parseLong(memberIdStr);
            MemberRole role = MemberRole.valueOf(roleStr);
            MemberState state = MemberState.valueOf(stateStr);

            log.debug("Resolved AuthenticatedUser - memberId: {}, role: {}, state: {}",
                     memberId, role, state);

            return AuthenticatedUser.of(memberId, role, state);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid auth headers. memberId={}, role={}, state={}", memberIdStr, roleStr, stateStr, e);
            throw new CustomException(ErrorCode.AUTH_CONTEXT_NOT_FOUND);
        }
    }
}