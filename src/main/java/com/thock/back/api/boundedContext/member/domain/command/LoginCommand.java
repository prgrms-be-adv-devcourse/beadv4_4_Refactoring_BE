package com.thock.back.api.boundedContext.member.domain.command;

public record LoginCommand(
        String email,
        String password
) {}
