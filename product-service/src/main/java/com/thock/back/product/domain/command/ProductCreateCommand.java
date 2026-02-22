package com.thock.back.product.domain.command;

import com.thock.back.product.domain.Category;
import com.thock.back.shared.member.domain.MemberRole;

import java.util.Map;

public record ProductCreateCommand(
        Long sellerId,
        MemberRole role,
        String name,
        Long price,
        Long salePrice,
        Integer stock,
        Category category,
        String description,
        String imageUrl,
        Map<String, Object> detail
) {}
