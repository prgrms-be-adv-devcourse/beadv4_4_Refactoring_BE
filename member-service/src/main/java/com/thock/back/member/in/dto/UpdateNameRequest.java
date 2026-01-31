package com.thock.back.member.in.dto;

public record UpdateNameRequest(
        String bankCode,
        String accountNumber,
        String accountHolder
) {}
