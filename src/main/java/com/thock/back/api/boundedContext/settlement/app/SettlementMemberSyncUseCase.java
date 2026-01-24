package com.thock.back.api.boundedContext.settlement.app;

import com.thock.back.api.boundedContext.settlement.domain.SettlementMember;
import com.thock.back.api.boundedContext.settlement.out.SettlementMemberRepository;
import com.thock.back.api.shared.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service; // 👈 이거 꼭 붙여주세요!
import org.springframework.transaction.annotation.Transactional;

@Service // 빈 등록 필수
@RequiredArgsConstructor
public class SettlementMemberSyncUseCase {

    private final SettlementMemberRepository settlementMemberRepository;

    @Transactional
    public void syncMember(MemberDto memberDto) { // 메서드 이름 소문자 시작 권장 (camelCase)

        settlementMemberRepository.findById(memberDto.getId())
                .ifPresentOrElse(
                        // 1. 이미 존재하면 -> 업데이트 (Update)
                        existingMember -> {
                            existingMember.update(
                                    memberDto.getEmail(),
                                    memberDto.getName(),
                                    memberDto.getRole(),
                                    memberDto.getState(),
                                    memberDto.getBankCode(),
                                    memberDto.getAccountNumber(),
                                    memberDto.getAccountHolder(),
                                    memberDto.getUpdatedAt() // 수정일 동기화
                            );
                            // Transactional 덕분에 save 호출 안 해도 자동 update 쿼리 나감 (더티 체킹)
                        },
                        // 2. 없으면 -> 새로 생성 (Insert)
                        () -> {
                            SettlementMember newMember = SettlementMember.builder() // 빌더 패턴 추천
                                    .id(memberDto.getId())
                                    .email(memberDto.getEmail())
                                    .name(memberDto.getName())
                                    .role(memberDto.getRole())
                                    .state(memberDto.getState())
                                    .bankCode(memberDto.getBankCode())
                                    .accountNumber(memberDto.getAccountNumber())
                                    .accountHolder(memberDto.getAccountHolder())
                                    .createdAt(memberDto.getCreatedAt())
                                    .updatedAt(memberDto.getUpdatedAt())
                                    .build();

                            settlementMemberRepository.save(newMember);
                        }
                );
    }
}