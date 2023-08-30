package com.aloha.time.service;

import com.aloha.time.entity.Member;
import com.aloha.time.model.AttendanceDto;
import com.aloha.time.repository.MemberRepository;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
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

        // 성공 >> https://learn.inha.ac.kr/
        // 실패 >> https://learn.inha.ac.kr/login.php?errorcode=3
        if(!resUrl.equals("https://learn.inha.ac.kr/")) token = "";

        return token;
    }

    public List<AttendanceDto> getAttendances(String token) throws IOException, ParseException {
        String url = "https://learn.inha.ac.kr/";

        Connection.Response res = Jsoup.connect(url)
                .cookie("MoodleSession", token)
                .method(Connection.Method.POST)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .execute();

        Document doc = res.parse();
        Elements myCourses = doc.select(".course_link");

        //key : 과목 Id, value = 과목명
        Map<String, String> mapCourseIdName = new HashMap<>();

        String courseId = "";
        String courseName = "";
        for (Element courseEl : myCourses) {
            courseId = courseEl.attr("abs:href").split("=")[1];
            courseName = courseEl.select(".course-name .course-title h3").text();
            mapCourseIdName.put(courseId, courseName);
        }

        List<AttendanceDto> attendanceDtoList = new ArrayList<>();
        AttendanceDto attendanceDto;

        if(!mapCourseIdName.isEmpty()) {
            url = "https://learn.inha.ac.kr/report/ubcompletion/user_progress_a.php?id=";

            String subjectName = "";
            for(String subjectId : mapCourseIdName.keySet()) {
                Connection.Response progressRes = Jsoup.connect(url + subjectId)
                        .cookie("MoodleSession", token)
                        .execute();

                Document progressDoc = progressRes.parse();
                Elements myProgress = progressDoc.select(".user_progress_table tbody tr");
                log.info("과목명 >> " + mapCourseIdName.get(subjectId));
                subjectName = mapCourseIdName.get(subjectId);
                String fromDate = "";
                String toDate = "";
                for (Element week : myProgress) {

                    Boolean isAttended = week.select("td").last().text().equals("O") ? true : false;
                    DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                    fromDate = week.select("td:nth-child(4) .btn.btn-default.btn-xs.track_detail").attr("data-sterm");
                    toDate = week.select("td:nth-child(4) .btn.btn-default.btn-xs.track_detail").attr("data-eterm");

                    if(!fromDate.equals("")) {
                        fromDate = formatter.format(Long.parseLong(fromDate) * 1000L);
                    }
                    if(!toDate.equals("")) {
                        toDate = formatter.format(Long.parseLong(toDate) * 1000L);
                    }

                    attendanceDto = new AttendanceDto();
                    attendanceDto.setSubjectName(subjectName);
                    attendanceDto.setAttendedDateTo(toDate);
                    attendanceDto.setAttendedDateFrom(fromDate);
                    attendanceDto.setIsAttended(isAttended);
                    attendanceDtoList.add(attendanceDto);
                }
            }
        }
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