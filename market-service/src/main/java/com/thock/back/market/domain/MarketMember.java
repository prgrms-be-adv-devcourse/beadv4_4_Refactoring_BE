package com.thock.back.market.domain;

import com.thock.back.shared.market.dto.MarketMemberDto;
import com.thock.back.shared.member.domain.MemberRole;
import com.thock.back.shared.member.domain.MemberState;
import com.thock.back.shared.member.domain.ReplicaMember;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "market_members")
@NoArgsConstructor
@Getter
public class MarketMember extends ReplicaMember {

    @Column(length = 6)
    private String zipCode;          // 우편번호 (5~6자)
    private String baseAddress;      // 기본 주소 (도로명/지번)
    private String detailAddress;    // 상세 주소 (동/호수 등)

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
    public void updateShippingAddress(String zipCode,
                                      String baseAddress,
                                      String detailAddress) {
        this.zipCode = zipCode;
        this.baseAddress = baseAddress;
        this.detailAddress = detailAddress;
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
        return new MarketMemberDto(
                getId(),
                getCreatedAt(),
                getUpdatedAt(),
                getEmail(),
                getName(),
                getRole(),
                getState(),
                getZipCode(),
                getBaseAddress(),
                getDetailAddress()
//                getBankCode(),
//                getAccountNumber(),
//                getAccountHolder()
        );
    }


}
