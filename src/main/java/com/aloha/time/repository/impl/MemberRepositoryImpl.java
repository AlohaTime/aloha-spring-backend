package com.aloha.time.repository.impl;

import com.aloha.time.entity.Member;
import com.aloha.time.repository.MemberRepositoryCustom;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.aloha.time.entity.QMember.member;

@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepositoryCustom {

    final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<Member> findMember(long id, String email) {
        return jpaQueryFactory.selectFrom(member).where(member.id.eq(id).and(member.email.eq(email))).fetch();
    }
}
