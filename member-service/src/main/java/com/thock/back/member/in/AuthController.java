package com.thock.back.member.in;

import com.thock.back.member.app.AuthApplicationService;
import com.thock.back.member.in.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "auth-controller", description = "인증 관련 API")
public class AuthController {

    private final AuthApplicationService authApplicationService;

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하여 토큰을 발급받습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (잘못된 이메일 또는 비밀번호)")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        AuthenticationResult result = authApplicationService.login(request.toCommand());

        return ResponseEntity.ok(new LoginResponse(result.accessToken(), result.refreshToken()));
    }

    @Operation(summary = "토큰 갱신", description = "RefreshToken을 사용하여 새로운 AccessToken을 발급받습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 RefreshToken")
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(
            @RequestBody TokenRefreshRequest request
    ) {
        AuthenticationResult result = authApplicationService.refreshAccessToken(request.refreshToken());
        return ResponseEntity.ok(new TokenRefreshResponse(result.accessToken(), result.refreshToken()));
    }
}

