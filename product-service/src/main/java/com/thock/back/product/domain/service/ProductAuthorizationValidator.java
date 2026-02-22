package com.thock.back.product.domain.service;

import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.product.domain.entity.Product;
import com.thock.back.shared.member.domain.MemberRole;
import org.springframework.stereotype.Component;

@Component
public class ProductAuthorizationValidator {

    public void validateSellerRole(MemberRole role) {
        if (role != MemberRole.SELLER) {
            throw new CustomException(ErrorCode.USER_FORBIDDEN);
        }
    }

    public void validateOwnership(Product product, Long requesterId, MemberRole role) {
        if (role != MemberRole.ADMIN && !product.getSellerId().equals(requesterId)) {
            throw new CustomException(ErrorCode.SELLER_FORBIDDEN);
        }
    }
}
