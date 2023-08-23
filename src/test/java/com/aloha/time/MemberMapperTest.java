package com.aloha.time;

import com.aloha.time.entity.Member;
import com.aloha.time.mapper.MemberMapper;
import com.aloha.time.model.MemberDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MemberMapperTest {

    private final MemberMapper memberMapper = Mappers.getMapper(MemberMapper.class);

    Member member;

    @BeforeEach
    public void beforeEach() {
        member = Member
                .builder()
                .id(1)
                .password("testpassword")
                .email("test@naver.com")
                .build();
    }

    @Test
    public void 멤버_매퍼_테스트() {
        MemberDto memberDto = memberMapper.toDto(member);

        Assertions.assertEquals(memberDto.getId(), member.getId());
        Assertions.assertEquals(memberDto.getPassword(), member.getPassword());
        Assertions.assertEquals(memberDto.getEmail(), member.getEmail());
    }


}
