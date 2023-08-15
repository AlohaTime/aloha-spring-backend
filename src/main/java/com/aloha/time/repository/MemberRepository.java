package com.aloha.time.repository;

import com.aloha.time.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository /* DB에 접근하여 쿼리문을 실행시키는 역할을 하도록 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    //TODO. 아이디 찾기, 비밀번호 찾기

    // 사용자 입력값 Id로 User Entity에서 찾아서 password 컬럼과 입력값 password와 비교
    List<Member> findByIdAndEmail(long id, String email); //Id와 Email로 Password 컬럼값 추출
    List<Member> findAllById(long id); //Id와 Email로 Password 컬럼값 추출
    List<Member> findByEmail(String email); //Id와 Email로 Password 컬럼값 추출
}