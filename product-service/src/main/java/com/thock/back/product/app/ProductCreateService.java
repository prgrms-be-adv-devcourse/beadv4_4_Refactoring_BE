package com.thock.back.product.app;

import com.thock.back.global.eventPublisher.EventPublisher;
import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.product.domain.command.ProductCreateCommand;
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
public class ProductCreateService {
    private final ProductRepository productRepository;
    private final EventPublisher eventPublisher;
    private final ProductAuthorizationValidator authorizationValidator;

    public Long createProduct(ProductCreateCommand command) {

        authorizationValidator.validateSellerRole(command.role());

        Product product = Product.builder()
                .sellerId(command.sellerId())
                .name(command.name())
                .price(command.price())
                .salePrice(command.salePrice())
                .stock(command.stock())
                .category(command.category())
                .description(command.description())
                .imageUrl(command.imageUrl())
                .detail(command.detail())
                .build();

        Product saved = productRepository.save(product);

        eventPublisher.publish(ProductEvent.builder()
                .productId(saved.getId())
                .sellerId(saved.getSellerId())
                .eventType(ProductEventType.CREATE)
                .build());

        return saved.getId();
    }
}
