package com.thock.back.api.boundedContext.member.domain;

public record SignUpCommand(
        String email,
        String name,
        String password
) {}
