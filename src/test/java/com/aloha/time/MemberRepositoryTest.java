package com.aloha.time;

import com.aloha.time.config.TestConfig;
import com.aloha.time.entity.Member;
import com.aloha.time.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;


// H2 기반으로 테스트용 DB 를 구축 후 테스트하고,
// 테스트가 끝나면 트랜잭션 롤백
@Import({TestConfig.class})
@DataJpaTest
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @DisplayName("사용자 추가")
    @Test
    void addUser() {
        // given
        Member member = Member.builder().email("test@naver.com").password("rtttt").build();

        // when
        Member savedMember = memberRepository.save(member);

        // then
        assertThat(savedMember.getId()).isNotZero();
        assertThat(savedMember.getEmail()).isEqualTo(member.getEmail());
        assertThat(savedMember.getPassword()).isEqualTo(member.getPassword());
    }

}
