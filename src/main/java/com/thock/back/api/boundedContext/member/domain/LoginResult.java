package com.thock.back.api.boundedContext.member.domain;

public record LoginResult(
        String accessToken,
        String refreshToken
) {}

