package com.thock.back.api.boundedContext.member.domain;

public record LoginCommand(
        String email,
        String password
) {}
