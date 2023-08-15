package com.aloha.time.service;

import com.aloha.time.entity.Member;
import com.aloha.time.repository.MemberRepository;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;

    public List<Member> getUser(Long id, String email) {
        // 왜 getUser 인데 List 로 조회 ?

        // id 가 있는 경우
        // email o -> 둘 다로 조회
        // email x -> id 로 조회

        // id 가 없는 경우
        // email o -> 이메일로 조회
        // email x -> 모든 데이터 조회
        if (id != null && StringUtils.isBlank(email)) {
            return memberRepository.findByIdAndEmail(id, email);
        } else if (id != null) {
            return memberRepository.findAllById(id);
        } else if (StringUtils.isNotBlank(email)) {
            return memberRepository.findByEmail(email);
        }
        return memberRepository.findAll();
    }

    public String createUser(String password, String email) {
        Member member = Member
                .builder()
                .email(email)
                .password(password)
                .build();

        memberRepository.save(member);
        return "회원가입 완료";
    }
}