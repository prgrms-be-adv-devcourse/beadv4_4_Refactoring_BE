package com.thock.back.payment.domain.dto.response;


import com.thock.back.payment.domain.RevenueLog;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class RevenueLogResponseDto {
    private Long memberId;
    private Long walletId;
    private List<RevenueLog> revenueLog;
}
