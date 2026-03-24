package com.thock.back.member.domain.command;

public record PromoteToSellerCommand(
        Long memberId,
        String bankCode,
        String accountNumber,
        String accountHolder
) {}
