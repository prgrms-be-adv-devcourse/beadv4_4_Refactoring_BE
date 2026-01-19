package com.thock.back.api.boundedContext.member.in;

import com.thock.back.api.boundedContext.member.app.AuthApplicationService;
import com.thock.back.api.boundedContext.member.domain.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
public class AuthController {

    private final AuthApplicationService authApplicationService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) throws Exception {

        LoginResult result = authApplicationService.login(new LoginCommand(request.email(), request.password()));

        return ResponseEntity.ok(new LoginResponse(result.accessToken(), result.refreshToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(@RequestBody TokenRefreshRequest request) throws Exception {
        String accessToken = authApplicationService.refreshAccessToken(request.refreshToken());
        return ResponseEntity.ok(new TokenRefreshResponse(accessToken));
    }
}

