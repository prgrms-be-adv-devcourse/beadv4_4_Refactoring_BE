package com.thock.back.api.boundedContext.member.domain.command;

public record SignUpCommand(
        String email,
        String name,
        String password
) {}
