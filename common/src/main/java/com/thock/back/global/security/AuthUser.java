package com.thock.back.global.security;

import java.lang.annotation.*;

/**
 * Gateway에서 검증된 인증 정보를 주입받기 위한 애노테이션
 *
 * 사용 예시:
 * <pre>
 * {@code
 * @GetMapping("/me")
 * public ResponseEntity<MemberInfoResponse> getMyInfo(@AuthUser AuthenticatedUser user) {
 *     Long memberId = user.memberId();
 *     MemberRole role = user.role();
 *     // ...
 * }
 * }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthUser {
}