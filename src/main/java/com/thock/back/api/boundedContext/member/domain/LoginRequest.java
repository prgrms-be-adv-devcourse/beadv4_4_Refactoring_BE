package com.thock.back.api.boundedContext.member.domain;

public record LoginRequest(
        String email,
        String password
) {}
