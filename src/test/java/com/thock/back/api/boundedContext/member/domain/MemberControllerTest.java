//package com.thock.back.api.boundedContext.member.domain;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.thock.back.api.boundedContext.member.app.MemberSignUpService;
//import com.thock.back.api.boundedContext.member.domain.command.SignUpCommand;
//import com.thock.back.api.boundedContext.member.in.dto.SignUpRequest;
//import com.thock.back.api.global.exception.CustomException;
//import com.thock.back.api.global.exception.ErrorCode;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.verify;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@SpringBootTest
//@AutoConfigureMockMvc(addFilters = false)
//class MemberControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @MockitoBean
//    private MemberSignUpService memberSignUpService;
//
//    @Test
//    @DisplayName("정상적인 회원가입 요청 시 201 Created를 반환한다")
//    void signUp_Success() throws Exception {
//        // given
//        SignUpRequest request = new SignUpRequest(
//                "test@example.com",
//                "테스트유저",
//                "password123"
//        );
//        Long expectedMemberId = 1L;
//
//        given(memberSignUpService.signUp(any(SignUpCommand.class)))
//                .willReturn(expectedMemberId);
//
//        // when & then
//        mockMvc.perform(post("/api/v1/members/signup")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.memberId").value(expectedMemberId));
//
//        verify(memberSignUpService).signUp(any(SignUpCommand.class));
//    }
//
//    @Test
//    @DisplayName("중복된 이메일로 가입 시 예외가 발생한다")
//    void signUp_DuplicateEmail_ThrowsException() throws Exception {
//        // given
//        SignUpRequest request = new SignUpRequest(
//                "duplicate@example.com",
//                "테스트유저",
//                "password123"
//        );
//
//        given(memberSignUpService.signUp(any(SignUpCommand.class)))
//                .willThrow(new CustomException(ErrorCode.MEMBER_EMAIL_ALREADY_EXISTS));
//
//        // when & then
//        mockMvc.perform(post("/api/v1/members/signup")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().is4xxClientError());
//
//        verify(memberSignUpService).signUp(any(SignUpCommand.class));
//    }
//}