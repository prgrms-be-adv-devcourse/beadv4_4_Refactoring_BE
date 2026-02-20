package com.thock.back.member.in.dto;

import com.thock.back.member.domain.command.PromoteToSellerCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateRoleRequest(
        @NotBlank(message = "은행 코드는 필수입니다")
        String bankCode,

        @NotBlank(message = "계좌번호는 필수입니다")
        @Pattern(regexp = "^\\d{10,14}$", message = "계좌번호는 10-14자리 숫자여야 합니다")
        String accountNumber,

        @NotBlank(message = "예금주명은 필수입니다")
        @Size(min = 2, max = 50, message = "예금주명은 2-50자여야 합니다")
        String accountHolder
) {
    public PromoteToSellerCommand toCommand(Long memberId) {
        return new PromoteToSellerCommand(memberId, bankCode, accountNumber, accountHolder);
    }
}
