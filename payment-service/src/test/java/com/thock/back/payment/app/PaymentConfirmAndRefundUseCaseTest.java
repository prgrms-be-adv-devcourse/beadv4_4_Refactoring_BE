package com.thock.back.payment.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thock.back.global.eventPublisher.EventPublisher;
import com.thock.back.global.exception.CustomException;
import com.thock.back.global.exception.ErrorCode;
import com.thock.back.global.security.JwtValidator;
import com.thock.back.payment.domain.*;
import com.thock.back.payment.domain.dto.request.PaymentConfirmRequestDto;
import com.thock.back.payment.out.PaymentMemberRepository;
import com.thock.back.payment.out.PaymentRepository;
import com.thock.back.payment.out.WalletRepository;
import com.thock.back.shared.member.domain.MemberRole;
import com.thock.back.shared.member.domain.MemberState;
import com.thock.back.shared.payment.dto.PaymentCancelRequestDto;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
        "spring.kafka.listener.auto-startup=false"
})
class PaymentConfirmAndRefundUseCaseTest {

    private static MockWebServer mockWebServer;

    @Autowired
    private PaymentConfirmAndRefundUseCase paymentConfirmAndRefundUseCase;

    @MockitoBean
    private PaymentRepository paymentRepository;

    @MockitoBean
    private PaymentMemberRepository paymentMemberRepository;

    @MockitoBean
    private WalletRepository walletRepository;

    @MockitoBean
    private EventPublisher eventPublisher;

    @MockitoBean
    private JwtValidator jwtValidator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PaymentMember testMember;
    private Payment testPayment;
    private Wallet testWallet;

    @BeforeAll
    static void beforeAll() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void afterAll() throws IOException {
        mockWebServer.shutdown();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("custom.payment.toss.baseUrl", () -> mockWebServer.url("/").toString().replaceAll("/$", ""));
        registry.add("custom.payment.toss.payments.secretKey", () -> "test_secret_key");
    }

    @BeforeEach
    void setUp() {
        // MockWebServer 응답 큐 초기화를 위해 Dispatcher 설정
        mockWebServer.setDispatcher(new okhttp3.mockwebserver.Dispatcher() {
            @Override
            public MockResponse dispatch(okhttp3.mockwebserver.RecordedRequest request) {
                return new MockResponse().setResponseCode(404);
            }
        });
        // 기본 Dispatcher로 복원 (enqueue 사용을 위해)
        mockWebServer.setDispatcher(new okhttp3.mockwebserver.QueueDispatcher());


        testMember = new PaymentMember(
                "test@test.com",
                "테스트유저",
                MemberState.ACTIVE,
                MemberRole.USER,
                1L,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null,
                null
        );
        ReflectionTestUtils.setField(testMember, "id", 1L);

        testPayment = new Payment(
                10000L,  // pgAmount
                testMember,
                PaymentStatus.REQUESTED,
                "ORDER_001",
                10000L,  // amount
                "paymentKey123"
        );

        ReflectionTestUtils.setField(testPayment, "id", 1L);
        ReflectionTestUtils.setField(testPayment, "createdAt", LocalDateTime.now());

        testWallet = new Wallet(testMember);
        ReflectionTestUtils.setField(testWallet, "id", 1L);
    }

    // ==================== confirmPayment 테스트 ====================

    @Test
    @DisplayName("결제 확인 성공")
    void confirmPayment_Success() throws Exception {
        // given
        PaymentConfirmRequestDto request = new PaymentConfirmRequestDto(
                "paymentKey123",
                "ORDER_001",
                10000L
        );

        when(paymentRepository.findByOrderId("ORDER_001")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentMemberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        when(walletRepository.findByHolderId(1L)).thenReturn(Optional.of(testWallet));

        Map<String, Object> tossResponse = Map.of(
                "paymentKey", "paymentKey123",
                "orderId", "ORDER_001",
                "totalAmount", 10000,
                "status", "DONE"
        );

        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(tossResponse))
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // when
        Map<String, Object> result = paymentConfirmAndRefundUseCase.confirmPayment(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("status")).isEqualTo("DONE");
        assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        verify(paymentRepository, atLeastOnce()).save(testPayment);
        verify(walletRepository, times(2)).save(testWallet);
        verify(eventPublisher, atLeastOnce()).publish(any());
    }

    @Test
    @DisplayName("결제 확인 실패 - 주문번호 없음")
    void confirmPayment_OrderNotFound() {
        // given
        PaymentConfirmRequestDto request = new PaymentConfirmRequestDto(
                "paymentKey123",
                "INVALID_ORDER",
                10000L
        );

        when(paymentRepository.findByOrderId("INVALID_ORDER")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentConfirmAndRefundUseCase.confirmPayment(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_UNKNOWN_ORDER_NUMBER);
    }

    @Test
    @DisplayName("결제 확인 실패 - 결제 상태가 REQUESTED가 아님")
    void confirmPayment_InvalidStatus() {
        // given
        testPayment.updatePaymentStatus(PaymentStatus.COMPLETED);

        PaymentConfirmRequestDto request = new PaymentConfirmRequestDto(
                "paymentKey123",
                "ORDER_001",
                10000L
        );

        when(paymentRepository.findByOrderId("ORDER_001")).thenReturn(Optional.of(testPayment));
        when(paymentMemberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        when(walletRepository.findByHolderId(1L)).thenReturn(Optional.of(testWallet));

        // when & then
        assertThatThrownBy(() -> paymentConfirmAndRefundUseCase.confirmPayment(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_NOT_REQUEST);
    }

    @Test
    @DisplayName("결제 확인 실패 - 금액 불일치")
    void confirmPayment_AmountMismatch() throws Exception {
        // given
        PaymentConfirmRequestDto request = new PaymentConfirmRequestDto(
                "paymentKey123",
                "ORDER_001",
                10000L
        );

        when(paymentRepository.findByOrderId("ORDER_001")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentMemberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        when(walletRepository.findByHolderId(1L)).thenReturn(Optional.of(testWallet));

        Map<String, Object> tossResponse = Map.of(
                "paymentKey", "paymentKey123",
                "orderId", "ORDER_001",
                "totalAmount", 5000,  // 금액 불일치
                "status", "DONE"
        );

        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(tossResponse))
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // when & then
        assertThatThrownBy(() -> paymentConfirmAndRefundUseCase.confirmPayment(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOSS_AMOUNT_NOT_MATCH);

        assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.REQUESTED);  // 상태 복원됨
    }

    // ==================== cancelToss 테스트 ====================

    @Test
    @DisplayName("토스 전액 환불 성공")
    void cancelToss_FullRefund_Success() throws Exception {
        // given
        testPayment.updatePaymentStatus(PaymentStatus.COMPLETED);

        PaymentCancelRequestDto request = new PaymentCancelRequestDto(
                "ORDER_001",
                "고객 요청",
                10000L
        );

        when(paymentRepository.findByOrderId("ORDER_001")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(walletRepository.findByHolderId(1L)).thenReturn(Optional.of(testWallet));

        Map<String, Object> tossResponse = Map.of(
                "paymentKey", "paymentKey123",
                "status", "CANCELED",
                "cancels", List.of(
                        Map.of("cancelAmount", 10000, "cancelReason", "고객 요청")
                )
        );

        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(tossResponse))
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // when
        paymentConfirmAndRefundUseCase.cancelToss(request);

        // then
        assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(testPayment.getRefundedAmount()).isEqualTo(10000L);
        verify(paymentRepository, atLeastOnce()).save(testPayment);
        verify(walletRepository).save(testWallet);
        verify(eventPublisher, atLeastOnce()).publish(any());
    }

    @Test
    @DisplayName("토스 부분 환불 성공")
    void cancelToss_PartialRefund_Success() throws Exception {
        // given
        testPayment.updatePaymentStatus(PaymentStatus.COMPLETED);

        PaymentCancelRequestDto request = new PaymentCancelRequestDto(
                "ORDER_001",
                "부분 환불 요청",
                5000L
        );

        when(paymentRepository.findByOrderId("ORDER_001")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(walletRepository.findByHolderId(1L)).thenReturn(Optional.of(testWallet));

        Map<String, Object> tossResponse = Map.of(
                "paymentKey", "paymentKey123",
                "status", "CANCELED",
                "cancels", List.of(
                        Map.of("cancelAmount", 5000, "cancelReason", "부분 환불 요청")
                )
        );

        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(tossResponse))
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // when
        paymentConfirmAndRefundUseCase.cancelToss(request);

        // then
        assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_CANCELED);
        assertThat(testPayment.getRefundedAmount()).isEqualTo(5000L);
        verify(paymentRepository, atLeastOnce()).save(testPayment);
        verify(walletRepository).save(testWallet);
    }

    @Test
    @DisplayName("토스 환불 실패 - 이미 취소된 결제")
    void cancelToss_AlreadyCanceled() {
        // given
        testPayment.updatePaymentStatus(PaymentStatus.CANCELED);

        PaymentCancelRequestDto request = new PaymentCancelRequestDto(
                "ORDER_001",
                "환불 요청",
                10000L
        );

        when(paymentRepository.findByOrderId("ORDER_001")).thenReturn(Optional.of(testPayment));
        when(walletRepository.findByHolderId(1L)).thenReturn(Optional.of(testWallet));

        // when & then
        assertThatThrownBy(() -> paymentConfirmAndRefundUseCase.cancelToss(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_NOT_COMPLETE);
    }

    @Test
    @DisplayName("토스 환불 실패 - 환불 사유 없음")
    void cancelToss_NoCancelReason() {
        // given
        testPayment.updatePaymentStatus(PaymentStatus.COMPLETED);

        PaymentCancelRequestDto request = new PaymentCancelRequestDto(
                "ORDER_001",
                null,
                10000L
        );

        when(paymentRepository.findByOrderId("ORDER_001")).thenReturn(Optional.of(testPayment));
        when(walletRepository.findByHolderId(1L)).thenReturn(Optional.of(testWallet));

        // when & then
        assertThatThrownBy(() -> paymentConfirmAndRefundUseCase.cancelToss(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFUND_NOT_CANCEL_REASON);
    }

    @Test
    @DisplayName("토스 환불 실패 - 금액이 0 이하")
    void cancelToss_InvalidAmount() {
        // given
        testPayment.updatePaymentStatus(PaymentStatus.COMPLETED);

        PaymentCancelRequestDto request = new PaymentCancelRequestDto(
                "ORDER_001",
                "환불 요청",
                0L
        );

        when(paymentRepository.findByOrderId("ORDER_001")).thenReturn(Optional.of(testPayment));
        when(walletRepository.findByHolderId(1L)).thenReturn(Optional.of(testWallet));

        // when & then
        assertThatThrownBy(() -> paymentConfirmAndRefundUseCase.cancelToss(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REFUND_AMOUNT);
    }

    // ==================== cancelPayment 테스트 ====================

    @Test
    @DisplayName("내부 결제 전액 환불 성공")
    void cancelPayment_FullRefund_Success() {
        // given
        testPayment.updatePaymentStatus(PaymentStatus.COMPLETED);

        PaymentCancelRequestDto request = new PaymentCancelRequestDto(
                "ORDER_001",
                "환불 요청",
                10000L
        );

        when(paymentRepository.findByOrderId("ORDER_001")).thenReturn(Optional.of(testPayment));
        when(walletRepository.findByHolderId(1L)).thenReturn(Optional.of(testWallet));

        // when
        paymentConfirmAndRefundUseCase.cancelPayment(request);

        // then
        assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(testPayment.getRefundedAmount()).isEqualTo(10000L);
        assertThat(testWallet.getBalance()).isEqualTo(10000L);
        verify(paymentRepository).save(testPayment);
        verify(walletRepository).save(testWallet);
        verify(eventPublisher, atLeastOnce()).publish(any());
    }

    @Test
    @DisplayName("내부 결제 부분 환불 성공")
    void cancelPayment_PartialRefund_Success() {
        // given
        testPayment.updatePaymentStatus(PaymentStatus.COMPLETED);

        PaymentCancelRequestDto request = new PaymentCancelRequestDto(
                "ORDER_001",
                "부분 환불",
                5000L
        );

        when(paymentRepository.findByOrderId("ORDER_001")).thenReturn(Optional.of(testPayment));
        when(walletRepository.findByHolderId(1L)).thenReturn(Optional.of(testWallet));

        // when
        paymentConfirmAndRefundUseCase.cancelPayment(request);

        // then
        assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_CANCELED);
        assertThat(testPayment.getRefundedAmount()).isEqualTo(5000L);
        assertThat(testWallet.getBalance()).isEqualTo(5000L);
        verify(paymentRepository).save(testPayment);
        verify(walletRepository).save(testWallet);
        verify(eventPublisher, atLeastOnce()).publish(any());
    }

    @Test
    @DisplayName("내부 결제 환불 실패 - 이미 취소된 결제")
    void cancelPayment_AlreadyCanceled() {
        // given
        testPayment.updatePaymentStatus(PaymentStatus.CANCELED);

        PaymentCancelRequestDto request = new PaymentCancelRequestDto(
                "ORDER_001",
                "환불 요청",
                10000L
        );

        when(paymentRepository.findByOrderId("ORDER_001")).thenReturn(Optional.of(testPayment));
        when(walletRepository.findByHolderId(1L)).thenReturn(Optional.of(testWallet));

        // when & then
        assertThatThrownBy(() -> paymentConfirmAndRefundUseCase.cancelPayment(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_NOT_COMPLETE);
    }

    @Test
    @DisplayName("내부 결제 환불 실패 - 금액이 0 이하")
    void cancelPayment_InvalidAmount() {
        // given
        testPayment.updatePaymentStatus(PaymentStatus.COMPLETED);

        PaymentCancelRequestDto request = new PaymentCancelRequestDto(
                "ORDER_001",
                "환불 요청",
                -1000L
        );

        when(paymentRepository.findByOrderId("ORDER_001")).thenReturn(Optional.of(testPayment));
        when(walletRepository.findByHolderId(1L)).thenReturn(Optional.of(testWallet));

        // when & then
        assertThatThrownBy(() -> paymentConfirmAndRefundUseCase.cancelPayment(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REFUND_AMOUNT);
    }

    @Test
    @DisplayName("내부 결제 환불 실패 - 주문번호 없음")
    void cancelPayment_OrderNotFound() {
        // given
        PaymentCancelRequestDto request = new PaymentCancelRequestDto(
                "INVALID_ORDER",
                "환불 요청",
                10000L
        );

        when(paymentRepository.findByOrderId("INVALID_ORDER")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentConfirmAndRefundUseCase.cancelPayment(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_UNKNOWN_ORDER_NUMBER);
    }

    @Test
    @DisplayName("내부 결제 환불 실패 - 지갑 없음")
    void cancelPayment_WalletNotFound() {
        // given
        testPayment.updatePaymentStatus(PaymentStatus.COMPLETED);

        PaymentCancelRequestDto request = new PaymentCancelRequestDto(
                "ORDER_001",
                "환불 요청",
                10000L
        );

        when(paymentRepository.findByOrderId("ORDER_001")).thenReturn(Optional.of(testPayment));
        when(walletRepository.findByHolderId(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentConfirmAndRefundUseCase.cancelPayment(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WALLET_NOT_FOUND);
    }

    @Test
    @DisplayName("내부 결제 환불 실패 - 환불 금액 초과")
    void cancelPayment_ExceedRefundAmount() {
        // given
        testPayment.updatePaymentStatus(PaymentStatus.COMPLETED);

        PaymentCancelRequestDto request = new PaymentCancelRequestDto(
                "ORDER_001",
                "환불 요청",
                15000L  // 결제 금액(10000)보다 큼
        );

        when(paymentRepository.findByOrderId("ORDER_001")).thenReturn(Optional.of(testPayment));
        when(walletRepository.findByHolderId(1L)).thenReturn(Optional.of(testWallet));

        // when & then
        assertThatThrownBy(() -> paymentConfirmAndRefundUseCase.cancelPayment(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REFUND_AMOUNT);
    }

    // ==================== 추가 테스트 케이스 ====================

    @Test
    @DisplayName("결제 확인 실패 - 멤버 조회 실패")
    void confirmPayment_MemberNotFound() {
        // given
        PaymentConfirmRequestDto request = new PaymentConfirmRequestDto(
                "paymentKey123",
                "ORDER_001",
                10000L
        );

        when(paymentRepository.findByOrderId("ORDER_001")).thenReturn(Optional.of(testPayment));
        when(paymentMemberRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentConfirmAndRefundUseCase.confirmPayment(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("결제 확인 실패 - 지갑 조회 실패")
    void confirmPayment_WalletNotFound() {
        // given
        PaymentConfirmRequestDto request = new PaymentConfirmRequestDto(
                "paymentKey123",
                "ORDER_001",
                10000L
        );

        when(paymentRepository.findByOrderId("ORDER_001")).thenReturn(Optional.of(testPayment));
        when(paymentMemberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        when(walletRepository.findByHolderId(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentConfirmAndRefundUseCase.confirmPayment(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WALLET_NOT_FOUND);
    }

    @Test
    @DisplayName("결제 확인 실패 - 토스 API 에러 응답")
    void confirmPayment_TossApiError() throws Exception {
        // given
        PaymentConfirmRequestDto request = new PaymentConfirmRequestDto(
                "paymentKey123",
                "ORDER_001",
                10000L
        );

        when(paymentRepository.findByOrderId("ORDER_001")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentMemberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        when(walletRepository.findByHolderId(1L)).thenReturn(Optional.of(testWallet));

        String errorResponse = "{\"code\":\"INVALID_REQUEST\",\"message\":\"잘못된 요청입니다.\"}";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody(errorResponse)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // when & then
        assertThatThrownBy(() -> paymentConfirmAndRefundUseCase.confirmPayment(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOSS_CONFIRM_FAIL);
    }

    @Test
    @DisplayName("토스 환불 실패 - 주문번호 없음")
    void cancelToss_OrderNotFound() {
        // given
        PaymentCancelRequestDto request = new PaymentCancelRequestDto(
                "INVALID_ORDER",
                "환불 요청",
                10000L
        );

        when(paymentRepository.findByOrderId("INVALID_ORDER")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentConfirmAndRefundUseCase.cancelToss(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_UNKNOWN_ORDER_NUMBER);
    }

    @Test
    @DisplayName("토스 환불 실패 - 지갑 없음 (성공 후 지갑 조회 실패)")
    void cancelToss_WalletNotFound() throws Exception {
        // given
        testPayment.updatePaymentStatus(PaymentStatus.COMPLETED);

        PaymentCancelRequestDto request = new PaymentCancelRequestDto(
                "ORDER_001",
                "환불 요청",
                10000L
        );

        when(paymentRepository.findByOrderId("ORDER_001")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(walletRepository.findByHolderId(1L)).thenReturn(Optional.empty());

        Map<String, Object> tossResponse = Map.of(
                "paymentKey", "paymentKey123",
                "status", "CANCELED",
                "cancels", List.of(
                        Map.of("cancelAmount", 10000, "cancelReason", "환불 요청")
                )
        );

        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(tossResponse))
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // when & then - 지갑 조회 실패는 별도 트랜잭션 내에서 발생
        assertThatThrownBy(() -> paymentConfirmAndRefundUseCase.cancelToss(request))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    @DisplayName("토스 환불 실패 - 결제 상태가 REQUESTED인 경우")
    void cancelToss_StatusIsRequested() {
        // given - testPayment는 기본적으로 REQUESTED 상태
        PaymentCancelRequestDto request = new PaymentCancelRequestDto(
                "ORDER_001",
                "환불 요청",
                10000L
        );

        when(paymentRepository.findByOrderId("ORDER_001")).thenReturn(Optional.of(testPayment));
        when(walletRepository.findByHolderId(1L)).thenReturn(Optional.of(testWallet));

        // when & then
        assertThatThrownBy(() -> paymentConfirmAndRefundUseCase.cancelToss(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_NOT_COMPLETE);
    }

    @Test
    @DisplayName("토스 환불 실패 - 토스 API 에러 응답")
    void cancelToss_TossApiError() throws Exception {
        // given
        testPayment.updatePaymentStatus(PaymentStatus.COMPLETED);

        PaymentCancelRequestDto request = new PaymentCancelRequestDto(
                "ORDER_001",
                "환불 요청",
                10000L
        );

        when(paymentRepository.findByOrderId("ORDER_001")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(walletRepository.findByHolderId(1L)).thenReturn(Optional.of(testWallet));

        String errorResponse = "{\"code\":\"ALREADY_CANCELED_PAYMENT\",\"message\":\"이미 취소된 결제입니다.\"}";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody(errorResponse)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // when & then
        assertThatThrownBy(() -> paymentConfirmAndRefundUseCase.cancelToss(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOSS_CONFIRM_FAIL);
    }

    @Test
    @DisplayName("내부 결제 환불 실패 - 결제 상태가 REQUESTED인 경우")
    void cancelPayment_StatusIsRequested() {
        // given - testPayment는 기본적으로 REQUESTED 상태
        PaymentCancelRequestDto request = new PaymentCancelRequestDto(
                "ORDER_001",
                "환불 요청",
                10000L
        );

        when(paymentRepository.findByOrderId("ORDER_001")).thenReturn(Optional.of(testPayment));
        when(walletRepository.findByHolderId(1L)).thenReturn(Optional.of(testWallet));

        // when & then
        assertThatThrownBy(() -> paymentConfirmAndRefundUseCase.cancelPayment(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_NOT_COMPLETE);
    }

    @Test
    @DisplayName("내부 결제 환불 실패 - 비활성 회원 지갑 입금 불가")
    void cancelPayment_InactiveWalletHolder() {
        // given
        PaymentMember inactiveMember = new PaymentMember(
                "inactive@test.com",
                "비활성유저",
                MemberState.INACTIVE,
                MemberRole.USER,
                2L,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null,
                null
        );
        ReflectionTestUtils.setField(inactiveMember, "id", 2L);

        Payment paymentWithInactiveMember = new Payment(
                10000L,
                inactiveMember,
                PaymentStatus.COMPLETED,
                "ORDER_002",
                10000L,
                "paymentKey456"
        );
        ReflectionTestUtils.setField(paymentWithInactiveMember, "id", 2L);
        ReflectionTestUtils.setField(paymentWithInactiveMember, "createdAt", LocalDateTime.now());

        Wallet inactiveWallet = new Wallet(inactiveMember);
        ReflectionTestUtils.setField(inactiveWallet, "id", 2L);

        PaymentCancelRequestDto request = new PaymentCancelRequestDto(
                "ORDER_002",
                "환불 요청",
                10000L
        );

        when(paymentRepository.findByOrderId("ORDER_002")).thenReturn(Optional.of(paymentWithInactiveMember));
        when(walletRepository.findByHolderId(2L)).thenReturn(Optional.of(inactiveWallet));

        // when & then
        assertThatThrownBy(() -> paymentConfirmAndRefundUseCase.cancelPayment(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WALLET_IS_LOCKED);
    }

    @Test
    @DisplayName("토스 환불 실패 - 환불 금액 초과")
    void cancelToss_ExceedRefundAmount() throws Exception {
        // given
        testPayment.updatePaymentStatus(PaymentStatus.COMPLETED);

        PaymentCancelRequestDto request = new PaymentCancelRequestDto(
                "ORDER_001",
                "환불 요청",
                15000L  // 결제 금액보다 큼
        );

        when(paymentRepository.findByOrderId("ORDER_001")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(walletRepository.findByHolderId(1L)).thenReturn(Optional.of(testWallet));

        // 토스에서는 성공 응답을 주지만, 내부 검증에서 실패
        Map<String, Object> tossResponse = Map.of(
                "paymentKey", "paymentKey123",
                "status", "CANCELED",
                "cancels", List.of(
                        Map.of("cancelAmount", 15000, "cancelReason", "환불 요청")
                )
        );

        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(tossResponse))
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // when & then
        assertThatThrownBy(() -> paymentConfirmAndRefundUseCase.cancelToss(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REFUND_AMOUNT);
    }

    // ==================== cancelBeforePayment 테스트 ====================

    @Test
    @DisplayName("결제 전 취소 성공")
    void cancelBeforePayment_Success() {
        // given
        when(paymentRepository.findByOrderId("ORDER_001")).thenReturn(Optional.of(testPayment));

        // when
        paymentConfirmAndRefundUseCase.cancelBeforePayment("ORDER_001");

        // then
        assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        verify(paymentRepository).save(testPayment);
    }

    @Test
    @DisplayName("결제 전 취소 실패 - 주문번호 없음")
    void cancelBeforePayment_OrderNotFound() {
        // given
        when(paymentRepository.findByOrderId("INVALID_ORDER")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentConfirmAndRefundUseCase.cancelBeforePayment("INVALID_ORDER"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_UNKNOWN_ORDER_NUMBER);
    }
}