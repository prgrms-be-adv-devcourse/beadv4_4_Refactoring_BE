package com.thock.back.shared.market.dto;

/**
 * Order가 생성될 때 Payment가 생성 됨
 * OrderState == CANCELLED 는 실제로 결제가 안 된 주문
 * 7일 간격으로 배치 처리하여 삭제할 예정
 * 결제를 하지 않았더라도 Order가 생성되는 시기에 Payment가 생김
 * Payment는 REQUESTED 혹은 PG_PENDING 으로 남아있음 이걸 삭제하라는 이벤트에 담을 DTO -> OrderDeleteRequestDto
 */
public record OrderDeleteRequestDto (
        String orderNumber
)
{ }
