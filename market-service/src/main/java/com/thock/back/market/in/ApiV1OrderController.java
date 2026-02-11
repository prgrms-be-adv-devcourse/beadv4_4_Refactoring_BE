package com.thock.back.market.in;


import com.thock.back.global.security.AuthUser;
import com.thock.back.global.security.AuthenticatedUser;
import com.thock.back.market.app.MarketFacade;
import com.thock.back.market.in.dto.req.OrderCancelRequest;
import com.thock.back.market.in.dto.req.OrderCreateRequest;
import com.thock.back.market.in.dto.req.OrderItemsCancelRequest;
import com.thock.back.market.in.dto.res.OrderCreateResponse;
import com.thock.back.market.in.dto.res.OrderDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/orders")
@Tag(name = "order-controller", description = "주문 관련 API")
public class ApiV1OrderController {
    private final MarketFacade marketFacade;

    @Operation(
            summary = "내 주문 목록 조회",
            description = "로그인한 사용자의 주문 내역을 최신순으로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping
    public ResponseEntity<List<OrderDetailResponse>> getMyOrders(@AuthUser AuthenticatedUser user) {
        Long memberId = user.memberId();
        log.info("Market Order API : getMyOrders / memberId = {}", memberId);
        List<OrderDetailResponse> orders = marketFacade.getMyOrders(memberId);
        return ResponseEntity.ok(orders);
    }

    @Operation(
            summary = "주문 상세 조회",
            description = "특정 주문의 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "본인의 주문이 아님"),
            @ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음")
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> getOrderDetail(
            @AuthUser AuthenticatedUser user,
            @PathVariable Long orderId) {
        Long memberId = user.memberId();
        log.info("Market Order API : getOrderDetail / memberId = {}", memberId);
        OrderDetailResponse order = marketFacade.getOrderDetail(memberId, orderId);
        return ResponseEntity.ok(order);
    }

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
    public ResponseEntity<OrderCreateResponse> createOrder(
            @AuthUser AuthenticatedUser user,
            @Valid @RequestBody OrderCreateRequest request) {
        Long memberId = user.memberId();
        log.info("Market Order API : createOrder / memberId = {}", memberId);
        OrderCreateResponse response = marketFacade.createOrder(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "주문 전체 취소",
            description = "주문 전체를 취소합니다. " +
                    "결제 완료된 주문의 경우 환불 처리가 진행됩니다. " +
                    "주문은 삭제되지 않고 취소 상태로 변경됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "주문 전체 취소 성공"),
            @ApiResponse(responseCode = "400", description = "취소 불가능한 주문 상태"),
            @ApiResponse(responseCode = "403", description = "본인의 주문이 아님"),
            @ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음")
    })
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @AuthUser AuthenticatedUser user,
            @PathVariable Long orderId,
            @RequestBody OrderCancelRequest request) {
        Long memberId = user.memberId();
        log.info("Market Order API : cancelOrder / memberId = {}, orderId = {}, request = {}", memberId, orderId, request);
        marketFacade.cancelOrder(memberId,orderId, request.cancelReasonType(), request.cancelReasonDetail());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "주문 상품 부분 취소",
            description = "주문 내 선택한 상품들을 취소합니다. " +
                    "단건 취소: [1], 다건 취소: [1, 2, 3], 결제 완료된 경우 해당 상품 금액만큼 부분 환불됩니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "삭제할 상품 ID 리스트",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "[1, 2, 3]")
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "주문 상품 부분 취소 성공"),
            @ApiResponse(responseCode = "400", description = "취소 불가능한 상태"),
            @ApiResponse(responseCode = "403", description = "본인의 주문이 아님"),
            @ApiResponse(responseCode = "404", description = "주문 또는 상품을 찾을 수 없음")
    })
    @PostMapping("/{orderId}/items/cancel")
    public ResponseEntity<Void> cancelOrderItem(
            @AuthUser AuthenticatedUser user,
            @PathVariable Long orderId,
            @RequestBody OrderItemsCancelRequest request
            ) {
        Long memberId = user.memberId();
        log.info("Market Order API : cancelOrderItem / memberId = {}, orderId = {}, request = {}", memberId, orderId, request);
        marketFacade.cancelOrderItems(
                memberId,
                orderId,
                request.orderItemIds(),
                request.cancelReasonType(),
                request.cancelReasonDetail()
        );
        return ResponseEntity.noContent().build();
    }
}
