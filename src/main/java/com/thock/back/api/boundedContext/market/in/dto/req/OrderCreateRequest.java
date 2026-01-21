package com.thock.back.api.boundedContext.market.in.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "주문 생성 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest {
    @Schema(description = "선택한 장바구니 아이템 ID 목록", example = "[1, 2, 3]")
    @NotEmpty(message = "주문할 상품을 선택해주세요.")
    private List<Long> cartItemIds;

    @Schema(description = "우편번호", example = "06234")
    @NotBlank(message = "우편번호는 필수입니다.")
    private String zipCode;

    @Schema(description = "기본 주소", example = "서울특별시 강남구 테헤란로 123")
    @NotBlank(message = "기본 주소는 필수입니다.")
    private String baseAddress;

    @Schema(description = "상세 주소", example = "ABC빌딩 4층")
    @NotBlank(message = "상세 주소는 필수입니다.")
    private String detailAddress;
}
