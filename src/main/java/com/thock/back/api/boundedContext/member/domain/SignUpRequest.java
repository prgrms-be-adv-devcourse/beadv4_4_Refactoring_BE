package com.thock.back.api.boundedContext.member.domain;

public record SignUpRequest(
        String email,
        String name,
        String password
) {}
