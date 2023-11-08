package com.aloha.time;

import com.aloha.time.controller.MemberController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

// 실제 애플리케이션을 로컬 위에 올려서 포트 주소 및 실제 DB와 커넥션이 붙어지는 상태에서 진행되는 Live 테스트 방법
// 즉, 실제 있는 코드를 직접적으로 실행하는 방법
@SpringBootTest
public class AlohaApplicationTest {

    @Autowired
    private MemberController memberController;

    @DisplayName("getUser API 테스트")
    @Test
    void 컨트롤러_로딩_테스트() {
        assert memberController != null;
    }

    @DisplayName("getUser API 호출 테스트")
    @Test
    void getUser_GET_테스트() {
        ResponseEntity<?> response = memberController.getUser(3L, "test@naver.com");
        assert response != null;
    }
}
