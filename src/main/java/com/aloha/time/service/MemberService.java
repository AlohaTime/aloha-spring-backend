package com.aloha.time.service;

import com.aloha.time.entity.Member;
import com.aloha.time.model.AssignmentDto;
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
import java.time.LocalDate;
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
        Map<String, String> mapCourseIdName = getCourse(token);
        List<AttendanceDto> attendanceDtoList = new ArrayList<>();
        AttendanceDto attendanceDto;

        if(!mapCourseIdName.isEmpty()) {
            String url = "https://learn.inha.ac.kr/report/ubcompletion/user_progress_a.php?id="; // 온라인 출석부
            // key : 동영상(강의)명, value : 출석여부(true:O / false:X)
            Map<String, Boolean> mapAttendance = new HashMap<>();

            for(String subjectId : mapCourseIdName.keySet()) {
                Connection.Response progressRes = Jsoup.connect(url + subjectId)
                        .cookie("MoodleSession", token)
                        .method(Connection.Method.GET)
                        .execute();

                Document progressDoc = progressRes.parse();
                Elements myProgress = progressDoc.select(".user_progress_table tbody tr");

                for (Element progressEl : myProgress) {
                    // 동영상의 제목과 온라인출석부의 강의자료 컬럼의
                    // 출석부에서 O, X로 가져올 때 td가 6개면(첫번째 로우라서) 2번째 컬럼 가져와야 함
                    // td가 4개면 첫번째 컬럼 가져와야 함(.text로 강의자료 값)

                    // map에 출석부에서 key : 강의명, value : 출석여부
                    int cellIdx = progressEl.select("td").size();
                    String mapKey = (cellIdx == 6) ? progressEl.select("td").get(1).text() : progressEl.select("td").first().text();
                    String mapValue = progressEl.select("td").last().text();;

                    if(!mapKey.equals("") && !mapValue.equals("") ) {
                        mapAttendance.put(mapKey, mapValue.equals("O") ? true : false);
                    }
                }
            }
            //log.info("mapAttendance >> " + mapAttendance);

            String testUrl = "https://learn.inha.ac.kr/mod/vod/index.php?id="; // + 과목Id로 과목의 동영상 목록을 가져옴
            for(String subjectId : mapCourseIdName.keySet()) {
                Connection.Response progressRes = Jsoup.connect(testUrl + subjectId)
                        .cookie("MoodleSession", token)
                        .method(Connection.Method.GET)
                        .execute();

                Document progressDoc = progressRes.parse();
                Elements myVideos = progressDoc.select(".generaltable tbody tr");

                if(!myVideos.isEmpty()) {
                    for(Element myVideo : myVideos) {
                        String videoUrl = myVideo.select(".cell.c1 a").attr("abs:href");
                        String lectureName = myVideo.select(".cell.c1 a").text();

                        if(!videoUrl.equals("") && !lectureName.equals("")) {
                            Connection.Response innerRes = Jsoup.connect(videoUrl)
                                    .cookie("MoodleSession", token)
                                    .method(Connection.Method.GET)
                                    .execute();

                            Document videoDoc = innerRes.parse();

                            String alertChk = videoDoc.select(".alert.alert-info h4 b").text();
                            // 진도체크가 설정된 콘텐츠는 기간 전에 학습이 불가
                            if(!alertChk.equals("학습불가")) {
                                Elements attendTermEls = videoDoc.select(".box.vod_info");
                                attendanceDto = new AttendanceDto();
                                attendanceDto.setSubjectName(mapCourseIdName.get(subjectId));
                                attendanceDto.setLectureName(lectureName);
                                attendanceDto.setIsAttended(mapAttendance.get(lectureName));

                                Boolean isAdd = true;
                                for(Element attendTermEl : attendTermEls) {
                                    String attendName = attendTermEl.select("span.vod_info").text(); // 시작일: 또는 출석인정일:이 나와야 함
                                    String attendValue = attendTermEl.select("span.vod_info_value").text();

                                    DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");
                                    Date attendDate = formatter.parse(attendValue);
                                    Date today = new Date();

                                    //출석인정기간 값 세팅
                                    if(attendName.equals("시작일:") && !attendDate.after(today)) {
                                        attendanceDto.setAttendedDateTo(attendValue);
                                    } else if(attendName.equals("출석인정기간:") && !attendDate.before(today)) {
                                        attendanceDto.setAttendedDateFrom(attendValue);
                                    } else {
                                        isAdd = false;
                                        break;
                                    }
                                }
                                if(isAdd) {
                                    attendanceDtoList.add(attendanceDto);
                                }
                            }

                        }
                    }

                } else {
                    //해당 과목은 동영상(온라인 출석)봐야할 게 없음
                    attendanceDto = new AttendanceDto();
                    attendanceDto.setSubjectName(mapCourseIdName.get(subjectId));
                    attendanceDto.setErrorMsg("동영상이 없습니다.");
                    attendanceDtoList.add(attendanceDto);
                }

            }
        }
        return attendanceDtoList;
    }
    public List<AssignmentDto> getAssignments(String token) throws IOException {
        Map<String, String> mapCourseIdName = getCourse(token);
        List<AssignmentDto> listAssignment = new ArrayList<>();
        AssignmentDto assignmentDto;

        if(!mapCourseIdName.isEmpty()) {
            String url = "https://learn.inha.ac.kr/mod/assign/index.php?id=";

            // index.php (과제명, 마감기한, 제출여부만 알 수 있음)
            Document indexPgDoc;
            Elements indexPgEls;
            // view.php (채점여부, 최종수정일시, 마감까지 남은 기한(n일)도 추가적으로 알 수 있음)
            Document viewPgDoc;
            Elements viewPgEls;

            for (String subjectId : mapCourseIdName.keySet()) {
                Connection.Response assignRes = Jsoup.connect(url + subjectId)
                        .cookie("MoodleSession", token)
                        .method(Connection.Method.GET)
                        .execute();

                indexPgDoc = assignRes.parse();

                if(!indexPgDoc.select(".generaltable tbody").hasClass("empty")) {
                    indexPgEls = indexPgDoc.select(".generaltable tbody tr");
                    // 과제 세부 페이지
                    for(Element assignEl : indexPgEls) {
                        String detailUrl = assignEl.select("a").attr("abs:href"); // view.php URL 링크 가져옴
                        if(!detailUrl.equals("")) {
                            Connection.Response assignDetailRes = Jsoup.connect(detailUrl)
                                    .cookie("MoodleSession", token)
                                    .method(Connection.Method.GET)
                                    .execute();

                            viewPgDoc = assignDetailRes.parse();
                            String assignName = viewPgDoc.select("#page-content-wrap h2").text();
                            viewPgEls = viewPgDoc.select(".generaltable tbody tr");

                            assignmentDto = new AssignmentDto();
                            assignmentDto.setSubjectName(mapCourseIdName.get(subjectId));
                            assignmentDto.setAssignName(assignName);
                            // 과제 세부정보 테이블
                            for(Element detailAssignEl : viewPgEls) {
                                String colName = detailAssignEl.select("td").first().text();
                                String colValue = detailAssignEl.select("td").last().text();

                                if(colName.equals("제출 여부")) {
                                    assignmentDto.setIsSubmit(colValue.equals("제출 안 함") ? false : true);
                                } else if(colName.equals("채점 상황")) {
                                    // 추후 DTO에 필드 추가 후 set
                                } else if(colName.equals("종료 일시")) {
                                    assignmentDto.setDueDate(colValue);
                                } else if(colName.equals("마감까지 남은 기한")) {
                                    // 추후 DTO에 필드 추가 후 set
                                } else if(colName.equals("최종 수정 일시")) {
                                    assignmentDto.setSubmitDate(colValue);
                                }
                            }
                            listAssignment.add(assignmentDto);
                        }
                    }
                } else {
                    assignmentDto = new AssignmentDto();
                    assignmentDto.setSubjectName(mapCourseIdName.get(subjectId));
                    assignmentDto.setErrorMsg("과제가 없습니다.");
                    listAssignment.add(assignmentDto);
                }
            }
        }

        return listAssignment;
    }

    public Map<String, String> getCourse(String token) throws IOException {
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

        return mapCourseIdName;
    }
}