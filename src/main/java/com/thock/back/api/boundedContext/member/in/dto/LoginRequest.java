package com.thock.back.api.boundedContext.member.in.dto;

public record LoginRequest(
        String email,
        String password
) {}
