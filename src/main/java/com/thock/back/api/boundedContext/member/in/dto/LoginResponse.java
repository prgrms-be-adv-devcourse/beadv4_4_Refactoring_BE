package com.thock.back.api.boundedContext.member.in.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {}
