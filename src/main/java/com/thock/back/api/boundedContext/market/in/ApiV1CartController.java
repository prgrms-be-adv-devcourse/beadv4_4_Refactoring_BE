package com.thock.back.api.boundedContext.market.in;

import com.thock.back.api.boundedContext.market.app.MarketFacade;
import com.thock.back.api.boundedContext.market.in.dto.req.CartItemAddRequest;
import com.thock.back.api.boundedContext.market.in.dto.res.CartItemListResponse;
import com.thock.back.api.boundedContext.market.in.dto.res.CartItemResponse;
import com.thock.back.api.global.security.AuthContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/carts")
public class ApiV1CartController {
    private final MarketFacade marketFacade;

    @Operation(
            summary = "장바구니 조회",
            description = "사용자의 장바구니에 담긴 상품 목록과 총 금액 정보를 조회합니다. " +
                    "각 상품의 가격, 할인가, 수량 정보와 함께 전체 합계 금액을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "장바구니 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CartItemListResponse.class))),
            @ApiResponse(responseCode = "404", description = "장바구니를 찾을 수 없음 (사용자 미존재 또는 장바구니 미생성)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류 (상품 정보 조회 실패 등)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<CartItemListResponse> getCartItems() throws Exception {
        Long memberId = AuthContext.memberId();
        CartItemListResponse response = marketFacade.getCartItems(memberId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "장바구니 상품 추가",
            description = "장바구니에 상품을 추가합니다. 상품 ID와 수량을 입력받아 장바구니에 추가한 후, " +
                    "추가된 상품의 상세 정보를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "장바구니 상품 추가 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CartItemResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자 또는 장바구니를 찾을 수 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류 (상품 정보 조회 실패 등)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    /**
     * 엔드포인트에 {productId} 추가하고 PathVariable로 받아도 된다.
     * 추후 상품의 옵션이 있는 경우도 고려하여 request에 함께 넣어서 보냄
     */
    @PostMapping("/items")
    public ResponseEntity<CartItemResponse> addCartItem(@Valid @RequestBody CartItemAddRequest request) throws Exception {
        Long memberId = AuthContext.memberId();
        CartItemResponse response = marketFacade.addCartItem(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
