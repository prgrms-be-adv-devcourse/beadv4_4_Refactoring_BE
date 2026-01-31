package com.thock.back.member.in;


import com.thock.back.global.exception.ErrorResponse;
import com.thock.back.member.app.AuthApplicationService;
import com.thock.back.member.domain.command.LoginCommand;
import com.thock.back.member.in.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Operation(summary = "로그인", description = "이메일과 비밀번호를 이용하여 로그인을 진행합니다. " + "로그인 성공 시 Access Token과 Refresh Token을 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (이메일 또는 비밀번호 불일치)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) throws Exception {

        AuthenticationResult result = authApplicationService.login(new LoginCommand(request.email(), request.password()));

        return ResponseEntity.ok(new LoginResponse(result.accessToken(), result.refreshToken()));
    }

    @Operation(summary = "토큰 재발급 (Refresh Token Rotation)", description = "Refresh Token을 이용하여 새로운 Access Token과 Refresh Token을 발급합니다. " + "보안 강화를 위해 Refresh Token Rotation 패턴을 적용하여 기존 Refresh Token은 폐기됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Access Token 재발급 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenRefreshResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (Refresh Token 만료 또는 유효하지 않음)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (Refresh Token 누락 등)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(@RequestBody TokenRefreshRequest request) throws Exception {
        AuthenticationResult result = authApplicationService.refreshAccessToken(request.refreshToken());
        return ResponseEntity.ok(new TokenRefreshResponse(result.accessToken(), result.refreshToken()));
    }
}

