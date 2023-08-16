package com.aloha.time.mapper;

import com.aloha.time.entity.Member;
import com.aloha.time.model.MemberDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MemberMapper extends GenericMapper<Member, MemberDto> {
}
