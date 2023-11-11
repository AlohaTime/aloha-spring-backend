package com.aloha.time;

import com.aloha.time.controller.MemberController;
import com.aloha.time.entity.Member;
import com.aloha.time.model.MemberCreateRequest;
import com.aloha.time.service.MemberService;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Junit5 + Mockito 를 연동해서 사용
@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

    @InjectMocks // 가짜 객체 주입
    private MemberController memberController;

    @Mock // 가짜 객체 생성
    private MemberService memberService;

    private MockMvc mockMvc; // HTTP 호출을 위해서 사용

    @BeforeEach
    public void init() {
        mockMvc = MockMvcBuilders.standaloneSetup(memberController)
                .build();
    }

    @DisplayName("회원 가입 성공")
    @Test
    void signUpSuccess() throws Exception {
        // given
        MemberCreateRequest request = MemberCreateRequest.builder()
                .password("test@test.test")
                .email("test")
                .build();

        // doReturn : 가짜 객체가 특정한 값을 반환해야 하는 경우
        // memberService 객체에 createUser 메소드를 호출했을 때 '회원가입 완료' 가 노출되어야 한다
        doReturn("회원가입 완료").when(memberService)
                .createUser(request.getPassword(), request.getEmail());

        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post("/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new Gson().toJson(request)));

        // then
        // 실제 status 가 200 인지 확인
        MvcResult mvcResult = resultActions.andExpect(status().isOk())
                .andReturn();
    }

    @DisplayName("사용자 목록 조회")
    @Test
    void getUserList() throws Exception {
        // given
        doReturn(memberList()).when(memberService)
                .getUser(any(Long.class), any(String.class));

        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get("/members?id=" + 1 + "&email=test@naver.com"));

        // then
        MvcResult mvcResult = resultActions.andExpect(status().isOk())
                .andReturn();

        List response = new Gson().fromJson(mvcResult.getResponse()
                .getContentAsString(), List.class);
        assertThat(response.size()).isEqualTo(5);
    }

    private List<Member> memberList() {
        List<Member> userList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            userList.add(Member.builder()
                    .password("testPwd" + i)
                    .email("test" + i + "@naver.com")
                    .build());
        }
        return userList;
    }

}
