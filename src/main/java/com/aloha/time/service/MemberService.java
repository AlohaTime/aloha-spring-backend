package com.aloha.time.service;

import com.aloha.time.entity.Member;
import com.aloha.time.model.ApiResponse;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {
    @Value("${connection.iclass-init-url}")
    private String loginUrl;
    @Value("${connection.iclass-video-url}")
    private String videoUrl;
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
    public ApiResponse loginUser(String memberId, String memberPw) {
        //String url = "https://learn.inha.ac.kr/login/index.php";
        Map<String, String> data = new HashMap<>();
        data.put("username", memberId);
        data.put("password", memberPw);

        String token = "";
        String resUrl = "";

        // 로그인 할 때 Connection
        try {
            Connection.Response res = Jsoup.connect(loginUrl).data(data).method(Connection.Method.POST)
                   .header("Content-Type", "application/x-www-form-urlencoded")
                   .execute();
            token = res.cookie("MoodleSession");
            resUrl = res.url().toString();
        } catch (IOException e) {
            return new ApiResponse(500, e.getMessage(), null);
        }

        String successUrl = "https://learn.inha.ac.kr/"; // 로그인 성공 시, URL
        if(!successUrl.equals(resUrl)) {
            return new ApiResponse(303, "로그인 실패", null);
        } else {
            return new ApiResponse(200, "로그인 성공", token);
        }
    }

    public ApiResponse getAttendances(String token) {
        ApiResponse courseRes = getCourse(token);
        List<AttendanceDto> attendanceDtoList = new ArrayList<>();
        AttendanceDto attendanceDto;
        Map<String, String> mapCourseIdName = (Map<String, String>)courseRes.getData();
        if(!mapCourseIdName.isEmpty()) {
            String url = "https://learn.inha.ac.kr/report/ubcompletion/user_progress_a.php?id="; // 온라인 출석부
            // key : 동영상(강의)명, value : 출석여부(true:O / false:X)
            Map<String, Boolean> mapAttendance = new HashMap<>();

            try {
                for(String subjectId : mapCourseIdName.keySet()) {
                    ApiResponse subjectRes = doConnectByToken(url + subjectId, token);
                    if(subjectRes.getData() != null) {
                        Connection.Response progressRes = (Connection.Response) subjectRes.getData();
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
                    } else {
                        return subjectRes;
                    }
                    // TODO. Connection할 떄, 문제가 있다면 null로 올 수도 있을 것 같은데 그 전에 catch에서 잡힐듯?

                }

                //String testUrl = "https://learn.inha.ac.kr/mod/vod/index.php?id="; // + 과목Id로 과목의 동영상 목록을 가져옴
                for(String subjectId : mapCourseIdName.keySet()) {
                    ApiResponse videoRes = doConnectByToken(videoUrl + subjectId, token);
                    if(videoRes.getData() != null) {
                        Connection.Response connVideoRes = (Connection.Response) videoRes.getData();
                        Document progressDoc = connVideoRes.parse();
                        Elements myVideos = progressDoc.select(".generaltable tbody tr");

                        if(!myVideos.isEmpty()) {
                            for(Element myVideo : myVideos) {
                                String videoUrl = myVideo.select(".cell.c1 a").attr("abs:href");
                                String lectureName = myVideo.select(".cell.c1 a").text();

                                if(!videoUrl.equals("") && !lectureName.equals("")) {
                                    ApiResponse innerVideoRes = doConnectByToken(videoUrl, token);
                                    Connection.Response innerRes = (Connection.Response) innerVideoRes.getData();
                                /*Connection.Response innerRes = Jsoup.connect(videoUrl)
                                        .cookie("MoodleSession", token)
                                        .method(Connection.Method.GET)
                                        .execute();*/

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
                                            //Date attendDate = formatter.parse(attendValue);
                                            //Date today = new Date();

                                            if(attendName.equals("시작일:")) attendanceDto.setAttendedDateTo(attendValue);
                                            if(attendName.equals("출석인정기간:")) attendanceDto.setAttendedDateFrom(attendValue);
                                            //금주 출석 -> 전체 출석
                                            /*if(attendName.equals("시작일:") && !attendDate.after(today)) {
                                                attendanceDto.setAttendedDateTo(attendValue);
                                            } else if(attendName.equals("출석인정기간:") && !attendDate.before(today)) {
                                                attendanceDto.setAttendedDateFrom(attendValue);
                                            } else {
                                                isAdd = false;
                                                break;
                                            }*/
                                        }
                                        if(isAdd) {
                                            attendanceDtoList.add(attendanceDto);
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        return videoRes;
                    }
                   /* Connection.Response progressRes = Jsoup.connect()
                            .cookie("MoodleSession", token)
                            .method(Connection.Method.GET)
                            .execute();*/

                    //다 getData() null체크하기
                }
                return new ApiResponse(200, "Success", attendanceDtoList);
            } catch(IOException e) {
                return new ApiResponse(500, e.getMessage(), null);
            }/* catch(ParseException e) {
                return new ApiResponse(404, e.getMessage(), null);
            }*/
        } else {
            return new ApiResponse(404, "수강신청된 과목이 없습니다.", null);
        }
        //return attendanceDtoList;
    }
    public ApiResponse getAssignments(String token) {
        ApiResponse courseRes = getCourse(token);
        List<AssignmentDto> listAssignment = new ArrayList<>();
        AssignmentDto assignmentDto;
        Map<String, String> mapCourseIdName = (Map<String, String>)courseRes.getData();
        if(!mapCourseIdName.isEmpty()) {
            String url = "https://learn.inha.ac.kr/mod/assign/index.php?id=";
            // index.php (과제명, 마감기한, 제출여부만 알 수 있음)
            Document indexPgDoc;
            Elements indexPgEls;
            // view.php (채점여부, 최종수정일시, 마감까지 남은 기한(n일)도 추가적으로 알 수 있음)
            Document viewPgDoc;
            Elements viewPgEls;

            try {
                log.info("getAssignments >> " + mapCourseIdName);
                for (String subjectId : mapCourseIdName.keySet()) {
                    ApiResponse subjectRes = doConnectByToken(url + subjectId, token);
                    if(subjectRes.getData() != null) {
                        Connection.Response assignRes = (Connection.Response) subjectRes.getData();
                        indexPgDoc = assignRes.parse();

                        if(!indexPgDoc.select(".generaltable tbody").hasClass("empty")) {
                            indexPgEls = indexPgDoc.select(".generaltable tbody tr");
                            // 과제 세부 페이지
                            for(Element assignEl : indexPgEls) {
                                String detailUrl = assignEl.select("a").attr("abs:href"); // view.php URL 링크 가져옴
                                if(!detailUrl.equals("")) {
                                    ApiResponse detailRes = doConnectByToken(detailUrl, token);
                                    if(detailRes.getData() != null) {
                                        Connection.Response assignDetailRes = (Connection.Response) detailRes.getData();
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

                                            if(colName.equals("채점 상황")) {
                                                // 추후 DTO에 필드 추가 후 set
                                            } else if(colName.equals("종료 일시")) {
                                                assignmentDto.setDueDate(colValue);
                                            } else if(colName.equals("마감까지 남은 기한")) {
                                                // 추후 DTO에 필드 추가 후 set
                                            } else if(colName.equals("최종 수정 일시") && colValue != null) {
                                                if(colValue.equals("-")) {
                                                    colValue = "";
                                                }
                                                assignmentDto.setSubmitDate(colValue);
                                            }
                                        }
                                        listAssignment.add(assignmentDto);
                                    } else {
                                        return detailRes;
                                    }
                                }
                            }
                        } else {
                            assignmentDto = new AssignmentDto();
                            assignmentDto.setSubjectName(mapCourseIdName.get(subjectId));
                            //assignmentDto.setErrorMsg("과제가 없습니다.");
                            listAssignment.add(assignmentDto);
                        }
                    } else {
                        return subjectRes;
                    }
                }
            } catch(IOException e) {
                return new ApiResponse(500, e.getMessage(), null);
            }
            return new ApiResponse(200, "Success", listAssignment);
        } else {
            return new ApiResponse(404, "수강신청된 과목이 없습니다.", null);
        }
    }

    public ApiResponse getCourse(String token) {
        try {
            String url = "https://learn.inha.ac.kr/";
            ApiResponse courseRes = doConnectByToken(url, token);
            if(courseRes.getData() != null) {
                Connection.Response res = (Connection.Response) courseRes.getData();
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
                log.info("mapCourseIdName >> " + mapCourseIdName);
                return new ApiResponse(200, "Success", mapCourseIdName);
            } else {
                return courseRes;
            }

        } catch(IOException e) {
            return new ApiResponse(500, e.getMessage(), null);
        }
    }

    // 로그인을 제외한 Connection에서 사용
    private ApiResponse doConnectByToken(String url, String token) {
        try {
            Connection.Response res = Jsoup.connect(url)
                .cookie("MoodleSession", token)
                .method(Connection.Method.POST)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .execute();

            return new ApiResponse(200, "Success", res);
        } catch(IOException e) {
            return new ApiResponse(500, e.getMessage(), null);
        }
    }
}