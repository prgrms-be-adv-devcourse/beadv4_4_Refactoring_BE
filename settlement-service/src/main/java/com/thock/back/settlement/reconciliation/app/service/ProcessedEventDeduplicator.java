package com.thock.back.settlement.reconciliation.app.service;

import com.thock.back.settlement.reconciliation.domain.ProcessedEvent;
import com.thock.back.settlement.reconciliation.in.dto.OrderItemMessageDto;
import com.thock.back.settlement.reconciliation.out.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcessedEventDeduplicator {

    private final ProcessedEventRepository processedEventRepository;

    // 멱등키 조합이 이미 존재할 경우
    public boolean isDuplicateAndMark(OrderItemMessageDto dto, String source) {
        String idempotencyKey = makeIdempotencyKey(dto, source);
        try {
            processedEventRepository.save(ProcessedEvent.builder()
                    .idempotencyKey(idempotencyKey)
                    .source(source)
                    .eventType(dto.eventType())
                    .orderNo(dto.orderNo())
                    .build());
            return false;
        } catch (DataIntegrityViolationException e) {
            return true;
        }
    }

    // 컬럼 조합으로 멱등키 생성
    private String makeIdempotencyKey(OrderItemMessageDto dto, String source) {
        return source + "|" +
                dto.orderNo() + "|" +
                dto.productId() + "|" +
                dto.eventType() + "|" +
                dto.snapshotAt() + "|" +
                dto.paymentAmount() + "|" +
                dto.productQuantity();
    }
}
