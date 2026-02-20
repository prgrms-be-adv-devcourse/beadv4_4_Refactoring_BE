package com.thock.back.product.in.dto;

import com.thock.back.product.domain.Category;
import com.thock.back.product.domain.command.ProductCreateCommand;
import com.thock.back.shared.member.domain.MemberRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Map;

public record ProductCreateRequest(
        @NotBlank(message = "상품명은 필수입니다")
        String name,

        @NotNull(message = "가격은 필수입니다")
        @Positive(message = "가격은 0보다 커야 합니다")
        Long price,

        @PositiveOrZero(message = "할인가는 0 이상이어야 합니다")
        Long salePrice,

        @PositiveOrZero(message = "재고는 0 이상이어야 합니다")
        Integer stock,

        @NotNull(message = "카테고리는 필수입니다")
        Category category,

        String description,
        String imageUrl,
        Map<String, Object> detail
) {
        /*
         * Request DTO를 Domain Command로 변환
         * Controller의 보일러플레이트 코드를 제거하기 위한 메서드
         */
        public ProductCreateCommand toCommand(Long sellerId, MemberRole role) {
                return new ProductCreateCommand(
                        sellerId,
                        role,
                        name,
                        price,
                        salePrice,
                        stock,
                        category,
                        description,
                        imageUrl,
                        detail
                );
        }
}
