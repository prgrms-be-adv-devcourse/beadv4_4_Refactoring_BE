package com.thock.back.member.in;


import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.global.exception.ErrorResponse;
import com.thock.back.global.security.AuthUser;
import com.thock.back.global.security.AuthenticatedUser;
import com.thock.back.member.app.MemberSignUpService;
import com.thock.back.member.app.MemberPromoteService;
import com.thock.back.member.domain.command.PromoteToSellerCommand;
import com.thock.back.member.domain.command.SignUpCommand;
import com.thock.back.member.in.dto.MemberInfoResponse;
import com.thock.back.member.in.dto.SignUpRequest;
import com.thock.back.member.in.dto.SignUpResponse;
import com.thock.back.member.in.dto.UpdateRoleRequest;
import com.thock.back.shared.member.domain.MemberRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@Valid @RequestBody SignUpRequest request) {

        Long memberId = memberSignUpService.signUp(
                new SignUpCommand(
                        request.email(),
                        request.name(),
                        request.password())
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SignUpResponse(memberId));

    }

    @GetMapping("/me")
    public ResponseEntity<MemberInfoResponse> getMyInfo(@AuthUser AuthenticatedUser user) {

        Long memberId = user.memberId();
        MemberRole role = user.role();

        return ResponseEntity.ok(new MemberInfoResponse(memberId, role));

    }

    @PatchMapping("/role")
    public ResponseEntity<?> updateRole(
            @AuthUser AuthenticatedUser user,
            @RequestBody UpdateRoleRequest request) {

        Long memberId = user.memberId();

        if (memberId == null) {
            throw new CustomException(ErrorCode.AUTH_CONTEXT_NOT_FOUND);
        }

        memberPromoteService.promoteToSeller(
                new PromoteToSellerCommand(
                        memberId,
                        request.bankCode(),
                        request.accountNumber(),
                        request.accountHolder())
        );

        return ResponseEntity.ok().build();

    }
}
