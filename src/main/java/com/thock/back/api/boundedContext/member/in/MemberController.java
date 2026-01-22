package com.thock.back.api.boundedContext.member.in;

import com.thock.back.api.boundedContext.member.app.MemberSignUpService;
import com.thock.back.api.boundedContext.member.domain.command.SignUpCommand;
import com.thock.back.api.boundedContext.member.in.dto.SignUpRequest;
import com.thock.back.api.boundedContext.member.in.dto.SignUpResponse;
import com.thock.back.api.global.exception.ErrorResponse;
import com.thock.back.api.global.security.AuthContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Tag(name = "member-controller", description = "회원 관련 API")
public class MemberController {

    private final MemberSignUpService memberSignUpService;


    @Operation(summary = "회원 가입", description = "이메일, 이름, 비밀번호를 이용하여 회원 가입을 진행합니다. " + "가입이 완료되면 생성된 회원의 ID를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원 가입 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SignUpResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패, 이메일 중복 등)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody SignUpRequest request) {
        Long memberId = memberSignUpService.signUp(
                new SignUpCommand(request.email(), request.name(), request.password())
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SignUpResponse(memberId));
    }

    @Operation(summary = "내 회원 ID 조회", description = "현재 로그인된 사용자의 회원 ID를 조회합니다. " + "JWT 또는 인증 컨텍스트를 기반으로 회원을 식별합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원 ID 조회 성공", content = @Content(mediaType = "text/plain", schema = @Schema(example = "1"))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/me")
    public String test() throws Exception {
        return AuthContext.memberId().toString();
    }
}
