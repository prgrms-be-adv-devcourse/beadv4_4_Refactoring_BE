package com.thock.back.member.domain.command;

public record SignUpCommand(
        String email,
        String name,
        String password
) {}
