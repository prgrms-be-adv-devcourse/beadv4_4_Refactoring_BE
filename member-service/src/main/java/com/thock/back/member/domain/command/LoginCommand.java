package com.thock.back.member.domain.command;

public record LoginCommand(
        String email,
        String password
) {}
