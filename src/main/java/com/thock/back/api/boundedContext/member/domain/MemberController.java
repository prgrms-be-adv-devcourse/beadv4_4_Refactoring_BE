package com.thock.back.api.boundedContext.member.domain;

import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members/signup")
@RequiredArgsConstructor
public class MemberController {

    private final MemberSignUpService memberSignUpService;

    @PostMapping
    public ResponseEntity<?> signUp(@RequestBody SignUpRequest request) {
        Long memberId = memberSignUpService.signUp(
                new SignUpCommand(request.email(), request.name(), request.password())
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SignUpResponse(memberId));
    }
}
