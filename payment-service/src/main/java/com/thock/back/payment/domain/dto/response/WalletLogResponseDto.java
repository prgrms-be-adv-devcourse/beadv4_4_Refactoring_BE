package com.thock.back.payment.domain.dto.response;


import com.thock.back.payment.domain.WalletLog;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class WalletLogResponseDto {
    private Long memberId;
    private Long walletId;
    private List<WalletLog> walletLog;
}
