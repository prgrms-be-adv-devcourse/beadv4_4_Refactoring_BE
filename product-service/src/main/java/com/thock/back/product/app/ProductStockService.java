package com.thock.back.product.app;

import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.product.domain.entity.Product;
import com.thock.back.product.out.ProductRepository;
import com.thock.back.shared.market.domain.StockEventType;
import com.thock.back.shared.market.dto.StockOrderItemDto;
import com.thock.back.shared.market.event.MarketOrderStockChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductStockService {

    private final ProductRepository productRepository;

    // 주문의 재고 변경 이벤트 처리 (예약, 해제, 커밋)
    @Transactional
    public  void handle(MarketOrderStockChangedEvent event) {

        // 이벤트 유효성 검사
        if (event == null || event.items() == null || event.items().isEmpty()) {
            return;
        }

        // 이벤트에 포함된 상품 ID 추출 및 정렬
        List<Long> ids = event.items().stream()
                .map(StockOrderItemDto::productId)
                .distinct()
                .sorted()
                .toList();

        // 상품 조회 및 PESSIMISTIC_WRITE 락 획득
        List<Product> products = productRepository.findAllByIdInForUpdate(ids);

        // 조회된 상품을 ID 기준으로 맵으로 변환
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // 이벤트에 포함된 모든 상품이 존재하는지 검증
        for (Long id : ids) {
            if (!productMap.containsKey(id)) {
                throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
            }
        }

        // 이벤트 타입에 따라 상품의 재고 상태 변경
        for (StockOrderItemDto item : event.items()) {
            Product product = productMap.get(item.productId());
            Integer qty = item.quantity();

            StockEventType type = event.eventType();
            if (type == StockEventType.RESERVE) {
                product.reserve(qty);
            } else if (type == StockEventType.RELEASE) {
                product.release(qty);
            } else if (type == StockEventType.COMMIT) {
                product.commit(qty);
            } else {
                throw new CustomException(ErrorCode.INVALID_REQUEST);
            }

        }

    }
}
