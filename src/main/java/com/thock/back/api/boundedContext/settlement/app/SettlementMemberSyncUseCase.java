package com.thock.back.api.boundedContext.settlement.app;

import com.thock.back.api.boundedContext.settlement.domain.SettlementMember;
import com.thock.back.api.boundedContext.settlement.out.SettlementMemberRepository;
import com.thock.back.api.shared.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementMemberSyncUseCase {

    private final SettlementMemberRepository settlementMemberRepository;

    @Transactional
    public void syncMember(MemberDto memberDto) {
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
                        },
                        () -> {
                            SettlementMember newMember = SettlementMember.builder()
                                    .id(memberDto.getId()) // ID 그대로 사용 (중요)
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