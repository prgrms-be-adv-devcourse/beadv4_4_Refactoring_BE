package com.thock.back.product.app;

import com.thock.back.global.eventPublisher.EventPublisher;
import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.product.domain.command.ProductUpdateCommand;
import com.thock.back.product.domain.entity.Product;
import com.thock.back.product.domain.service.ProductAuthorizationValidator;
import com.thock.back.product.out.ProductRepository;
import com.thock.back.shared.member.domain.MemberRole;
import com.thock.back.shared.product.event.ProductEvent;
import com.thock.back.shared.product.event.ProductEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductManageService {

    private final ProductRepository productRepository;
    private final EventPublisher eventPublisher;
    private final ProductAuthorizationValidator authorizationValidator;

    public Long updateProduct(ProductUpdateCommand command) {
        Product product = productRepository.findById(command.productId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        authorizationValidator.validateOwnership(product, command.requesterId(), command.role());

        product.modify(
                command.name(),
                command.price(),
                command.salePrice(),
                command.stock(),
                command.category(),
                command.description(),
                command.imageUrl(),
                command.detail()
        );

        eventPublisher.publish(ProductEvent.builder()
                .productId(product.getId())
                .sellerId(product.getSellerId())
                .name(product.getName())
                .price(product.getPrice())
                .salePrice(product.getSalePrice())
                .stock(product.getStock())
                .imageUrl(product.getImageUrl())
                .productState(product.getState().name())
                .eventType(ProductEventType.UPDATE)
                .build());

        return product.getId();
    }

    public void deleteProduct(Long productId, Long requesterId, MemberRole role) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        authorizationValidator.validateOwnership(product, requesterId, role);

        Long deletedId = product.getId();
        Long sellerId = product.getSellerId();

        productRepository.delete(product);

        eventPublisher.publish(ProductEvent.builder()
                .productId(deletedId)
                .sellerId(sellerId)
                .eventType(ProductEventType.DELETE)
                .build());
    }
}
