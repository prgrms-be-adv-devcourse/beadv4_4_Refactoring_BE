package com.thock.back.settlement.reconciliation.app.useCase;

import com.thock.back.settlement.reconciliation.domain.SalesLog;
import com.thock.back.settlement.reconciliation.domain.enums.OrderEventStatus;
import com.thock.back.settlement.reconciliation.in.dto.OrderItemMessageDto;
import com.thock.back.settlement.reconciliation.out.SalesLogRepository;
import com.thock.back.settlement.shared.enums.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class SaveSalesLogUseCase {
    private final SalesLogRepository salesLogRepository;
    public void execute(OrderItemMessageDto dto){
        OrderEventStatus eventStatus = OrderEventStatus.from(dto.eventType());
        switch (eventStatus) {

            case PAYMENT_COMPLETED -> {
                // 결제 완료건은 대사만 하고 정산은 하면 안되니 확정일자 기본값 NULL로 저장
                // 바로 Entity 전환 후 저장
                salesLogRepository.save(dto.toEntity());
            }

            case PURCHASE_CONFIRMED -> {
                // 확정일자가 정해짐으로써 정산 가능하게 됨
                // 부분 확정이 있을 수 있음.
                // 주문번호가 같은 여러 상품(한번에 결제한 상품들) 중에 특정 상품만 확정하는 경우가 있을 수 있으니
                // 상품 id를 포함해서 조회해야할듯(단건 조회)
                salesLogRepository.findByOrderNoAndProductIdAndTransactionType(
                        dto.orderNo(),
                        dto.productId(),
                        TransactionType.PAYMENT
                ).ifPresent(SalesLog::confirm);
            }

            case REFUND_COMPLETED -> {
                // 환불건은 바로 대사, 정산대상이 되어야함
                // 여기도 부분 환불이 있을 수 있지만, 새로운 주문 내역이 insert 되는 것이기에
                // 조회를 할 필요가 없으니 상관 X
                // 하지만 이렇게 되면 판매자는 물건을 팔지도 않았는데 돈을 뺏기는 상황이 나타남.
                // 상계처리를 하려면 구매 확정을 하지 않은 이전 기록에 확정일자를 새겨줘야함

                // 1. 환불 건은 확정일자를 지어 바로 정산 대상에 포함시킨다.
                SalesLog refundLog = dto.toEntity();
                refundLog.confirm();
                salesLogRepository.save(refundLog);

                // 2. 원본 결제 내역에도 확정일자를 지어 정산 대상에 포함시켜. 정산 시 +,- 0원 처리가 되게 한다.
                salesLogRepository.findByOrderNoAndProductIdAndTransactionType(
                        dto.orderNo(),
                        dto.productId(),
                        TransactionType.PAYMENT
                ).ifPresent(originalLog -> {
                    if(originalLog.getConfirmedAt() == null){
                        originalLog.confirm();
                    }
                });
            }
        }
    }
}
