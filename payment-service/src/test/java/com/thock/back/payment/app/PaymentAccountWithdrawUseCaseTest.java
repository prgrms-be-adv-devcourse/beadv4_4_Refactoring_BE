package com.thock.back.payment.app;

import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.global.security.JwtValidator;
import com.thock.back.payment.domain.PaymentMember;
import com.thock.back.payment.domain.Wallet;
import com.thock.back.payment.out.PaymentMemberRepository;
import com.thock.back.payment.out.WalletRepository;
import com.thock.back.shared.common.dto.DefaultResponseDto;
import com.thock.back.shared.member.domain.MemberRole;
import com.thock.back.shared.member.domain.MemberState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
        "spring.kafka.listener.auto-startup=false"
})
class PaymentAccountWithdrawUseCaseTest {

    @Autowired
    private PaymentAccountWithdrawUseCase paymentAccountWithdrawUseCase;

    @MockitoBean
    private PaymentMemberRepository paymentMemberRepository;

    @MockitoBean
    private WalletRepository walletRepository;

    @MockitoBean
    private JwtValidator jwtValidator;

    private PaymentMember sellerMember;
    private PaymentMember userMember;
    private PaymentMember inactiveMember;
    private Wallet sellerWallet;
    private Wallet userWallet;
    private Wallet inactiveWallet;

    @BeforeEach
    void setUp() {
        // 판매자 멤버
        sellerMember = new PaymentMember(
                "seller@test.com",
                "판매자",
                MemberState.ACTIVE,
                MemberRole.SELLER,
                1L,
                LocalDateTime.now(),
                LocalDateTime.now(),
                "088",
                "123456789",
                "홍길동"
        );
        ReflectionTestUtils.setField(sellerMember, "id", 1L);

        // 일반 유저 멤버
        userMember = new PaymentMember(
                "user@test.com",
                "일반유저",
                MemberState.ACTIVE,
                MemberRole.USER,
                2L,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null,
                null
        );
        ReflectionTestUtils.setField(userMember, "id", 2L);

        // 비활성 멤버
        inactiveMember = new PaymentMember(
                "inactive@test.com",
                "비활성유저",
                MemberState.INACTIVE,
                MemberRole.SELLER,
                3L,
                LocalDateTime.now(),
                LocalDateTime.now(),
                "088",
                "987654321",
                "김철수"
        );
        ReflectionTestUtils.setField(inactiveMember, "id", 3L);

        // 판매자 지갑 (revenue 있음)
        sellerWallet = new Wallet(sellerMember);
        ReflectionTestUtils.setField(sellerWallet, "id", 1L);
        ReflectionTestUtils.setField(sellerWallet, "revenue", 100000L);

        // 일반 유저 지갑
        userWallet = new Wallet(userMember);
        ReflectionTestUtils.setField(userWallet, "id", 2L);
        ReflectionTestUtils.setField(userWallet, "revenue", 50000L);

        // 비활성 멤버 지갑
        inactiveWallet = new Wallet(inactiveMember);
        ReflectionTestUtils.setField(inactiveWallet, "id", 3L);
        ReflectionTestUtils.setField(inactiveWallet, "revenue", 30000L);
    }

    @Nested
    @DisplayName("출금 성공 케이스")
    class SuccessCases {

        @Test
        @DisplayName("출금 성공 - 정상 케이스")
        void accountWithdraw_Success() {
            // given
            Long memberId = 1L;
            Long amount = 50000L;

            when(paymentMemberRepository.findById(memberId)).thenReturn(Optional.of(sellerMember));
            when(walletRepository.findByHolderId(memberId)).thenReturn(Optional.of(sellerWallet));

            // when
            DefaultResponseDto result = paymentAccountWithdrawUseCase.accountWithdraw(memberId, amount);

            // then
            assertThat(result).isNotNull();
            assertThat(result.body()).contains("50,000원 출금 신청이 완료되었습니다.");
            assertThat(sellerWallet.getRevenue()).isEqualTo(50000L); // 100000 - 50000
            verify(walletRepository).save(sellerWallet);
        }

        @Test
        @DisplayName("출금 성공 - 전액 출금")
        void accountWithdraw_FullAmount_Success() {
            // given
            Long memberId = 1L;
            Long amount = 100000L;

            when(paymentMemberRepository.findById(memberId)).thenReturn(Optional.of(sellerMember));
            when(walletRepository.findByHolderId(memberId)).thenReturn(Optional.of(sellerWallet));

            // when
            DefaultResponseDto result = paymentAccountWithdrawUseCase.accountWithdraw(memberId, amount);

            // then
            assertThat(result).isNotNull();
            assertThat(sellerWallet.getRevenue()).isEqualTo(0L);
            verify(walletRepository).save(sellerWallet);
        }

        @Test
        @DisplayName("출금 성공 - 최소 금액 (1000원) 출금")
        void accountWithdraw_MinAmount_Success() {
            // given
            Long memberId = 1L;
            Long amount = 1000L; // 최소 출금 금액

            when(paymentMemberRepository.findById(memberId)).thenReturn(Optional.of(sellerMember));
            when(walletRepository.findByHolderId(memberId)).thenReturn(Optional.of(sellerWallet));

            // when
            DefaultResponseDto result = paymentAccountWithdrawUseCase.accountWithdraw(memberId, amount);

            // then
            assertThat(result).isNotNull();
            assertThat(result.body()).contains("1,000원 출금 신청이 완료되었습니다.");
            assertThat(sellerWallet.getRevenue()).isEqualTo(99000L); // 100000 - 1000
            verify(walletRepository).save(sellerWallet);
        }
    }

    @Nested
    @DisplayName("출금 실패 케이스 - 멤버/지갑 조회")
    class MemberWalletFailCases {

        @Test
        @DisplayName("출금 실패 - 멤버 조회 실패")
        void accountWithdraw_MemberNotFound() {
            // given
            Long memberId = 999L;
            Long amount = 10000L;

            when(paymentMemberRepository.findById(memberId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> paymentAccountWithdrawUseCase.accountWithdraw(memberId, amount))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
        }

        @Test
        @DisplayName("출금 실패 - 지갑 조회 실패")
        void accountWithdraw_WalletNotFound() {
            // given
            Long memberId = 1L;
            Long amount = 10000L;

            when(paymentMemberRepository.findById(memberId)).thenReturn(Optional.of(sellerMember));
            when(walletRepository.findByHolderId(memberId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> paymentAccountWithdrawUseCase.accountWithdraw(memberId, amount))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WALLET_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("출금 실패 케이스 - 금액 검증")
    class AmountValidationFailCases {

        @Test
        @DisplayName("출금 실패 - 금액이 0")
        void accountWithdraw_ZeroAmount() {
            // given
            Long memberId = 1L;
            Long amount = 0L;

            when(paymentMemberRepository.findById(memberId)).thenReturn(Optional.of(sellerMember));
            when(walletRepository.findByHolderId(memberId)).thenReturn(Optional.of(sellerWallet));

            // when & then
            assertThatThrownBy(() -> paymentAccountWithdrawUseCase.accountWithdraw(memberId, amount))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_AMOUNT);
        }

        @Test
        @DisplayName("출금 실패 - 금액이 음수")
        void accountWithdraw_NegativeAmount() {
            // given
            Long memberId = 1L;
            Long amount = -10000L;

            when(paymentMemberRepository.findById(memberId)).thenReturn(Optional.of(sellerMember));
            when(walletRepository.findByHolderId(memberId)).thenReturn(Optional.of(sellerWallet));

            // when & then
            assertThatThrownBy(() -> paymentAccountWithdrawUseCase.accountWithdraw(memberId, amount))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_AMOUNT);
        }

        @Test
        @DisplayName("출금 실패 - revenue보다 큰 금액")
        void accountWithdraw_ExceedRevenue() {
            // given
            Long memberId = 1L;
            Long amount = 150000L; // revenue는 100000

            when(paymentMemberRepository.findById(memberId)).thenReturn(Optional.of(sellerMember));
            when(walletRepository.findByHolderId(memberId)).thenReturn(Optional.of(sellerWallet));

            // when & then
            assertThatThrownBy(() -> paymentAccountWithdrawUseCase.accountWithdraw(memberId, amount))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_AMOUNT);
        }

        @Test
        @DisplayName("출금 실패 - 최소 금액 미만 (999원)")
        void accountWithdraw_BelowMinAmount() {
            // given
            Long memberId = 1L;
            Long amount = 999L; // 최소 금액(1000원) 미만

            when(paymentMemberRepository.findById(memberId)).thenReturn(Optional.of(sellerMember));
            when(walletRepository.findByHolderId(memberId)).thenReturn(Optional.of(sellerWallet));

            // when & then
            assertThatThrownBy(() -> paymentAccountWithdrawUseCase.accountWithdraw(memberId, amount))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_AMOUNT);
        }

        @Test
        @DisplayName("출금 실패 - 최소 금액 미만 (1원)")
        void accountWithdraw_OneWon() {
            // given
            Long memberId = 1L;
            Long amount = 1L;

            when(paymentMemberRepository.findById(memberId)).thenReturn(Optional.of(sellerMember));
            when(walletRepository.findByHolderId(memberId)).thenReturn(Optional.of(sellerWallet));

            // when & then
            assertThatThrownBy(() -> paymentAccountWithdrawUseCase.accountWithdraw(memberId, amount))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_AMOUNT);
        }
    }

    @Nested
    @DisplayName("출금 실패 케이스 - 권한 검증")
    class RoleValidationFailCases {

        @Test
        @DisplayName("출금 실패 - 일반 유저는 출금 불가")
        void accountWithdraw_UserRoleNotAllowed() {
            // given
            Long memberId = 2L;
            Long amount = 10000L;

            when(paymentMemberRepository.findById(memberId)).thenReturn(Optional.of(userMember));
            when(walletRepository.findByHolderId(memberId)).thenReturn(Optional.of(userWallet));

            // when & then
            assertThatThrownBy(() -> paymentAccountWithdrawUseCase.accountWithdraw(memberId, amount))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ROLE_SELLER);
        }
    }

    @Nested
    @DisplayName("출금 실패 케이스 - 지갑 상태 검증")
    class WalletStateFailCases {

        @Test
        @DisplayName("출금 실패 - 비활성 멤버의 지갑에서 출금 불가")
        void accountWithdraw_InactiveMemberWallet() {
            // given
            Long memberId = 3L;
            Long amount = 10000L;

            when(paymentMemberRepository.findById(memberId)).thenReturn(Optional.of(inactiveMember));
            when(walletRepository.findByHolderId(memberId)).thenReturn(Optional.of(inactiveWallet));

            // when & then
            assertThatThrownBy(() -> paymentAccountWithdrawUseCase.accountWithdraw(memberId, amount))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WALLET_IS_LOCKED);
        }

        @Test
        @DisplayName("출금 실패 - revenue가 0인 경우")
        void accountWithdraw_ZeroRevenue() {
            // given
            Long memberId = 1L;
            Long amount = 10000L;

            Wallet zeroRevenueWallet = new Wallet(sellerMember);
            ReflectionTestUtils.setField(zeroRevenueWallet, "id", 4L);
            ReflectionTestUtils.setField(zeroRevenueWallet, "revenue", 0L);

            when(paymentMemberRepository.findById(memberId)).thenReturn(Optional.of(sellerMember));
            when(walletRepository.findByHolderId(memberId)).thenReturn(Optional.of(zeroRevenueWallet));

            // when & then
            assertThatThrownBy(() -> paymentAccountWithdrawUseCase.accountWithdraw(memberId, amount))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_AMOUNT);
        }

        @Test
        @DisplayName("출금 실패 - 탈퇴 멤버의 지갑에서 출금 불가")
        void accountWithdraw_WithdrawnMemberWallet() {
            // given
            PaymentMember withdrawnMember = new PaymentMember(
                    "withdrawn@test.com",
                    "탈퇴유저",
                    MemberState.WITHDRAWN,
                    MemberRole.SELLER,
                    4L,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    "088",
                    "111222333",
                    "이영희"
            );
            ReflectionTestUtils.setField(withdrawnMember, "id", 4L);

            Wallet withdrawnWallet = new Wallet(withdrawnMember);
            ReflectionTestUtils.setField(withdrawnWallet, "id", 4L);
            ReflectionTestUtils.setField(withdrawnWallet, "revenue", 50000L);

            Long memberId = 4L;
            Long amount = 10000L;

            when(paymentMemberRepository.findById(memberId)).thenReturn(Optional.of(withdrawnMember));
            when(walletRepository.findByHolderId(memberId)).thenReturn(Optional.of(withdrawnWallet));

            // when & then
            assertThatThrownBy(() -> paymentAccountWithdrawUseCase.accountWithdraw(memberId, amount))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WALLET_IS_LOCKED);
        }
    }

    @Nested
    @DisplayName("경계값 테스트")
    class BoundaryValueTests {

        @Test
        @DisplayName("출금 실패 - revenue와 동일한 금액 + 1원")
        void accountWithdraw_ExceedByOne() {
            // given
            Long memberId = 1L;
            Long amount = 100001L; // revenue(100000) + 1

            when(paymentMemberRepository.findById(memberId)).thenReturn(Optional.of(sellerMember));
            when(walletRepository.findByHolderId(memberId)).thenReturn(Optional.of(sellerWallet));

            // when & then
            assertThatThrownBy(() -> paymentAccountWithdrawUseCase.accountWithdraw(memberId, amount))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_AMOUNT);
        }

        @Test
        @DisplayName("출금 성공 - revenue와 정확히 동일한 금액")
        void accountWithdraw_ExactRevenue() {
            // given
            Long memberId = 1L;
            Long amount = 100000L; // revenue와 정확히 동일

            when(paymentMemberRepository.findById(memberId)).thenReturn(Optional.of(sellerMember));
            when(walletRepository.findByHolderId(memberId)).thenReturn(Optional.of(sellerWallet));

            // when
            DefaultResponseDto result = paymentAccountWithdrawUseCase.accountWithdraw(memberId, amount);

            // then
            assertThat(result).isNotNull();
            assertThat(sellerWallet.getRevenue()).isEqualTo(0L);
        }

        @Test
        @DisplayName("출금 성공 - revenue보다 1원 적은 금액")
        void accountWithdraw_OneLessThanRevenue() {
            // given
            Long memberId = 1L;
            Long amount = 99999L; // revenue(100000) - 1

            when(paymentMemberRepository.findById(memberId)).thenReturn(Optional.of(sellerMember));
            when(walletRepository.findByHolderId(memberId)).thenReturn(Optional.of(sellerWallet));

            // when
            DefaultResponseDto result = paymentAccountWithdrawUseCase.accountWithdraw(memberId, amount);

            // then
            assertThat(result).isNotNull();
            assertThat(sellerWallet.getRevenue()).isEqualTo(1L);
        }
    }
}
