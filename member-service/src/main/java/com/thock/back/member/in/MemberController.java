package com.thock.back.member.in;

import com.thock.back.global.security.AuthUser;
import com.thock.back.global.security.AuthenticatedUser;
import com.thock.back.member.app.MemberSignUpService;
import com.thock.back.member.app.MemberPromoteService;
import com.thock.back.member.in.dto.MemberInfoResponse;
import com.thock.back.member.in.dto.SignUpRequest;
import com.thock.back.member.in.dto.SignUpResponse;
import com.thock.back.member.in.dto.UpdateRoleRequest;
import com.thock.back.shared.member.domain.MemberRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    private final MemberPromoteService memberPromoteService;

    @Operation(summary = "회원 가입", description = "새로운 회원을 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원 가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 이메일")
    })
    @PostMapping("/signup")
    public ResponseEntity<?> signUp(
            @Valid @RequestBody SignUpRequest request
    ) {
        Long memberId = memberSignUpService.signUp(request.toCommand());

        return ResponseEntity.status(HttpStatus.CREATED).body(new SignUpResponse(memberId));
    }

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 회원의 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/me")
    public ResponseEntity<MemberInfoResponse> getMyInfo(
            @AuthUser AuthenticatedUser user
    ) {
        return ResponseEntity.ok(new MemberInfoResponse(user.memberId(), user.role()));
    }

    @Operation(summary = "판매자 권한 승격", description = "일반 회원을 판매자로 승격합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "승격 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (계좌 정보 누락 등)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "409", description = "이미 판매자 권한 보유")
    })
    @PatchMapping("/role")
    public ResponseEntity<?> updateRole(
            @AuthUser AuthenticatedUser user,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        memberPromoteService.promoteToSeller(request.toCommand(user.memberId()));

        return ResponseEntity.ok().build();
    }
}
