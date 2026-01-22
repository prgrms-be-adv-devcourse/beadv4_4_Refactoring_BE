package com.thock.back.api.boundedContext.member.in.dto;

public record LoginResult(
        String accessToken,
        String refreshToken
) {}

