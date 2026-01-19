package com.thock.back.api.boundedContext.member.domain;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {}
