package com.thock.back.api.boundedContext.member.in;

import com.thock.back.api.boundedContext.member.app.MemberSignUpService;
import com.thock.back.api.boundedContext.member.domain.SignUpCommand;
import com.thock.back.api.boundedContext.member.domain.SignUpRequest;
import com.thock.back.api.boundedContext.member.domain.SignUpResponse;
import com.thock.back.api.global.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberSignUpService memberSignUpService;

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody SignUpRequest request) {
        Long memberId = memberSignUpService.signUp(
                new SignUpCommand(request.email(), request.name(), request.password())
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SignUpResponse(memberId));
    }

    @GetMapping("/me")
    public String test() throws Exception {
        return AuthContext.memberId().toString();
    }

}
