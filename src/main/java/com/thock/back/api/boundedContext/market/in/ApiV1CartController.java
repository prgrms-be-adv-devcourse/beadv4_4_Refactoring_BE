package com.thock.back.api.boundedContext.market.in;

import com.thock.back.api.boundedContext.market.app.MarketFacade;
import com.thock.back.api.boundedContext.market.in.dto.res.CartItemListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/carts")
public class ApiV1CartController {
    private final MarketFacade marketFacade;

    // TODO : 인증 시스템 구현 필요
//    @Operation(
//            summary = "장바구니 조회",
//            description = "사용자의 장바구니에 담긴 상품 목록과 총 금액 정보를 조회합니다. " +
//                    "각 상품의 가격, 할인가, 수량 정보와 함께 전체 합계 금액을 반환합니다."
//    )
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "장바구니 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CartItemListResponse.class))),
//            @ApiResponse(responseCode = "404", description = "장바구니를 찾을 수 없음 (사용자 미존재 또는 장바구니 미생성)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
//            @ApiResponse(responseCode = "500", description = "서버 내부 오류 (상품 정보 조회 실패 등)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
//    })
//    @GetMapping
//    public ResponseEntity<CartItemListResponse> getCartItems(@AuthenticationPrincipal CustomUserDetails userDetails){
//        Long memberId = userDetails.getUserId();
//        CartItemListResponse response = marketFacade.getCartItems(memberId);
//        return ResponseEntity.ok(response);
//    }
}
