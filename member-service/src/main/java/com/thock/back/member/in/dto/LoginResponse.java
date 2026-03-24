package com.thock.back.member.in.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {}
