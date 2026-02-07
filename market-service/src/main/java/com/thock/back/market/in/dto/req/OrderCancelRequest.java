package com.thock.back.market.in.dto.req;

import com.thock.back.shared.market.domain.CancelReasonType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "주문 전체 취소 요청")
public record OrderCancelRequest (

        @Schema(description = "취소 사유 타입", example = "ETC")
        @NotNull(message = "취소 사유를 선택해주세요.")
        CancelReasonType cancelReasonType,

        @Schema(description = "취소 사유 상세(기타 선택 시)", example = "단순 변심으로 인한 취소")
        String cancelReasonDetail
)
{ }
