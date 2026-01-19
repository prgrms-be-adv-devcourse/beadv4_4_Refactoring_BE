package com.thock.back.api.boundedContext.member.in;

import com.thock.back.api.boundedContext.member.domain.LoginRequest;
import com.thock.back.api.boundedContext.member.domain.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {

        // TODO(다음 커밋): AuthService.login(request.email(), request.password())
        // 임시 응답(컴파일/동작 확인용)
        return ResponseEntity.ok(new LoginResponse("access-token", "refresh-token"));
    }
}
