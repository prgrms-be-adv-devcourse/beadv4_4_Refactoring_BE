package com.thock.back.market.domain;

import com.thock.back.shared.market.dto.MarketMemberDto;
import com.thock.back.shared.member.domain.MemberRole;
import com.thock.back.shared.member.domain.MemberState;
import com.thock.back.shared.member.domain.ReplicaMember;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "market_members")
@NoArgsConstructor
@Getter
public class MarketMember extends ReplicaMember {

    @Embedded
    private ShippingAddress shippingAddress;

//    // 계좌 정보 세분화
//    @Column(length = 10)
//    private String bankCode;         // 은행 코드
//    @Column(length = 50)
//    private String accountNumber;    // 계좌번호
//    @Column(length = 50)
//    private String accountHolder;    // 예금주명

    public MarketMember(String email,
                        String name,
                        MemberRole role,
                        MemberState state,
                        Long id,
                        LocalDateTime createdAt,
                        LocalDateTime updatedAt
                        ){
        super(email, name, role, state, id, createdAt, updatedAt);
        // 배송, 계좌 정보는 주문 시점에 입력 받으므로 null
    }

    // 배송지 정보 업데이트 메서드
    public void updateShippingAddress(ShippingAddress shippingAddress) {
        Objects.requireNonNull(shippingAddress, "shippingAddress must not be null");
        this.shippingAddress = new ShippingAddress(
                shippingAddress.getZipCode(),
                shippingAddress.getBaseAddress(),
                shippingAddress.getDetailAddress()
        );
    }

    // 계좌 정보 업데이트 메서드
//    public void updateAccountInfo(String bankCode,
//                                  String accountNumber,
//                                  String accountHolder) {
//        this.bankCode = bankCode;
//        this.accountNumber = accountNumber;
//        this.accountHolder = accountHolder;
//    }

    public MarketMemberDto toDto() {
        String zipCode = shippingAddress != null ? shippingAddress.getZipCode() : null;
        String baseAddress = shippingAddress != null ? shippingAddress.getBaseAddress() : null;
        String detailAddress = shippingAddress != null ? shippingAddress.getDetailAddress() : null;

        return new MarketMemberDto(
                getId(),
                getCreatedAt(),
                getUpdatedAt(),
                getEmail(),
                getName(),
                getRole(),
                getState(),
                zipCode,
                baseAddress,
                detailAddress
//                getBankCode(),
//                getAccountNumber(),
//                getAccountHolder()
        );
    }


}
