package com.thock.back.market.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ShippingAddress {

    @Column(name = "zip_code", length = 6)
    private String zipCode;         // 우편번호 (5~6자)

    @Column(name = "base_address")
    private String baseAddress;     // 기본 주소 (도로명/지번)

    @Column(name = "detail_address")
    private String detailAddress;   // 상세 주소 (동/호수 등)

    public ShippingAddress(String zipCode, String baseAddress, String
            detailAddress) {
        this.zipCode = zipCode;
        this.baseAddress = baseAddress;
        this.detailAddress = detailAddress;
    }
}
