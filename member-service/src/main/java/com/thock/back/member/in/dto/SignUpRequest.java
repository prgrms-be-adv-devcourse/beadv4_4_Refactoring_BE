package com.thock.back.member.in.dto;

public record SignUpRequest(
        String email,
        String name,
        String password
) {}
