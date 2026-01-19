package com.thock.back.api.boundedContext.member.domain;

public record LoginResponse(
        String acessToken,
        String refreshToken
) {}
