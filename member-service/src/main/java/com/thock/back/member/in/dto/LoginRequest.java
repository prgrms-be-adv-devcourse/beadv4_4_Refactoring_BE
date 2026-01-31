package com.thock.back.member.in.dto;

public record LoginRequest(
        String email,
        String password
) {}
