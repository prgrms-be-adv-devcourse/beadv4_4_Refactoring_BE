package com.thock.back.market.in.dto.req;

import com.thock.back.shared.market.domain.CancelReasonType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "주문 상품 부분 취소 요청")
public record OrderItemsCancelRequest (

        @Schema(description = "취소할 주문 상품 ID 목록", example = "[1, 2, 3]")
        @NotEmpty(message = "취소할 상품을 선택해주세요")
        List<@NotNull Long> orderItemIds,

        @Schema(description = "취소 사유 타입", example = "PRODUCT_DEFECT")
        @NotNull(message = "취소 사유를 선택해주세요.")
        CancelReasonType cancelReasonType,

        @Schema(description = "취소 사유 상세(기타 선택 시)", example = "상품 사이즈가 다르게 옴")
        String cancelReasonDetail
){

}
