package com.thock.back.api.boundedContext.market.in;

import com.thock.back.api.boundedContext.market.app.MarketFacade;
import com.thock.back.api.boundedContext.market.in.dto.req.OrderCreateRequest;
import com.thock.back.api.boundedContext.market.in.dto.res.OrderCreateResponse;
import com.thock.back.api.global.security.AuthContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
@Tag(name = "order-controller")
public class ApiV1OrderController {
    private final MarketFacade marketFacade;

    @Operation(
            summary = "주문 생성",
            description = "장바구니의 상품들로 주문을 생성합니다. " +
                    "주문 생성 시 상품 정보는 스냅샷으로 저장되며, 배송지 정보가 함께 저장됩니다. " +
                    "주문 생성 후 장바구니는 자동으로 비워집니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "주문 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패, 장바구니가 비어있음, 재고 부족 등)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자 또는 장바구니를 찾을 수 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류 (상품 정보 조회 실패 등)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<OrderCreateResponse> addCartItem(@Valid @RequestBody OrderCreateRequest request) throws Exception {
        Long memberId = AuthContext.memberId();
        OrderCreateResponse response = marketFacade.createOrder(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
