package com.thock.back.api.boundedContext.settlement.app;

import com.thock.back.api.boundedContext.settlement.domain.SettlementMember;
import com.thock.back.api.boundedContext.settlement.out.SettlementMemberRepository;
import com.thock.back.api.shared.member.event.SellerRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

//@Component
//@RequiredArgsConstructor
//public class SettlementMemberSyncUseCase {

//    private final SettlementMemberRepository settlementMemberRepository;
//
////    @Transactional
//    public void syncMember(SellerRegisteredEvent event){
////
////        if(settlementMemberRepository.existsById(event.getId())){
////            return;
////        }
////
////        SettlementMember member = SettlementMember.builder()
////                .id(event.getId())
////                //.. event 필드에 있는거 다 담기
////
////        settlementMemberRepository.save(syncMember());
////    }
////}
