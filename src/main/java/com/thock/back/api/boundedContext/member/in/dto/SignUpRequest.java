package com.thock.back.api.boundedContext.member.in.dto;

public record SignUpRequest(
        String email,
        String name,
        String password
) {}
