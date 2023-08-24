package com.aloha.time.service;

import com.aloha.time.entity.Member;
import com.aloha.time.model.AttendanceDto;
import com.aloha.time.model.MemberDto;
import com.aloha.time.repository.MemberRepository;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

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

    // 1. 로그인(토큰 가져오기)
    public String loginUser(String memberId, String memberPw) throws IOException {
        String url = "https://learn.inha.ac.kr/login/index.php";
        Map<String, String> data = new HashMap<>();
        data.put("username", memberId);
        data.put("password", memberPw);

        Connection.Response res = Jsoup.connect(url)
                .data(data)
                .method(Connection.Method.POST)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .execute();

        String token = res.cookie("MoodleSession");
        String resUrl = res.url().toString();

        if(!resUrl.equals("https://learn.inha.ac.kr/")) token = "";

        return token;
    }

    public List<AttendanceDto> getAttendances(String token) throws IOException {
        String url = "https://learn.inha.ac.kr/";

        Connection.Response res = Jsoup.connect(url)
                .cookie("MoodleSession", token)
                .method(Connection.Method.POST)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .execute();

        Document doc = res.parse();
        Elements myCourses = doc.select(".course_link");

        Set<String> setSubjectId = new HashSet<String>();

        for (Element courseEl : myCourses) {
            setSubjectId.add(courseEl.attr("abs:href").split("=")[1]);
        }

        List<AttendanceDto> attendanceDtoList = new ArrayList<>();
        AttendanceDto attendanceDto = new AttendanceDto();

        /*if(!setSubjectId.isEmpty()) {
            url = "https://learn.inha.ac.kr/report/ubcompletion/user_progress_a.php?id=";
            for(String subjectId : setSubjectId) {
                Connection.Response progressRes = Jsoup.connect(url + subjectId)
                        .cookie("MoodleSession", token)
                        .execute();

                Document progressDoc = progressRes.parse();
                Elements myProgress = progressDoc.select(".user_progress_table tbody tr");

                for (Element week : myProgress) {
                    if(week.children().size() != 6) continue;
                    String attendance = week.select("td").last().text();
                    System.out.println("attendance >> " + attendance);
                    //TODO. attendance가 'O'면 true? 확인
                    *//*attendanceDto.setMonth();
                    attendanceDto.setWeek();
                    attendanceDto.setSubjectName();
                    attendanceDto.setAttendedDate();
                    attendanceDto.setAttendedDateTo();
                    attendanceDto.setAttendedDateFrom();
                    attendanceDto.setIsAttended();*//*
                    //attendanceDtoList.add(attendanceDto);
                }

            }
        }*/
        return attendanceDtoList;
    }

    // TODO. Connection 공통 메서드로 만들어보기
    /*private Connection.Response getConnection(String url, MapString token) throws IOException {
        Connection.Response res = Jsoup.connect(url)
                .data(data)
                .method(Connection.Method.POST)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .execute();

        return connRes;
    }*/
}