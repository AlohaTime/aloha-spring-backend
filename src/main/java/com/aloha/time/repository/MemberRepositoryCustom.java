package com.aloha.time.repository;

import com.aloha.time.entity.Member;

import java.util.List;

public interface MemberRepositoryCustom {
    List<Member> findMember(long id, String email);
}
