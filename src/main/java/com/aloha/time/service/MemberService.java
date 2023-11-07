package com.aloha.time.service;

import com.aloha.time.model.ApiResponse;
import com.aloha.time.model.AssignmentDto;
import com.aloha.time.model.AttendanceDto;
import com.aloha.time.model.QuizDto;
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
    @Value("${connection.iclass-conn-success-url}")
    private String successUrl;
    @Value("${connection.iclass-init-url}")
    private String loginUrl;
    @Value("${connection.iclass-video-url}")
    private String videoUrl;
    @Value("${connection.iclass-assign-url}")
    private String assignUrl;
    @Value("${connection.iclass-quiz-url}")
    private String quizUrl;
    @Value("${connection.iclass-attend-url}")
    private String attendUrl;
    // 1. 로그인(토큰 가져오기)
    public ApiResponse loginUser(String memberId, String memberPw) {
        Map<String, String> data = new HashMap<>();
        data.put("username", memberId);
        data.put("password", memberPw);

        String token = "";
        String resUrl = "";
        Connection.Response res;

        // 로그인 할 때 Connection
        try {
            res = Jsoup.connect(loginUrl).data(data).method(Connection.Method.POST)
                   .header("Content-Type", "application/x-www-form-urlencoded")
                   .execute();
            token = res.cookie("MoodleSession");
            resUrl = res.url().toString();
        } catch (IOException e) {
            log.info("e.getMessage() >> " + e.getMessage());
            return new ApiResponse(500, e.getMessage(), null);
        }
        log.info("loginUser >> " + resUrl);
        //String successUrl = "https://learn.inha.ac.kr/"; // 로그인 성공 시, URL
        // 로그인 실패 URL -> https://learn.inha.ac.kr/login.php?errorcode=3
        if(successUrl.equals(resUrl)) {
            return new ApiResponse(res.statusCode(), "로그인 성공", token);
        } else if(resUrl.contains("errorcode=3")) {
            return new ApiResponse(401, "아이디 또는 패스워드가 잘못 입력되었습니다.", null);
        } else {
            return new ApiResponse(404, "알 수 없음", null);
        }
    }

    public ApiResponse getAttendances(String token) {
        ApiResponse apiResponse = getCourse(token);

        if(apiResponse.getData() != null) {
            Map<String, String> mapCourseIdName = (Map<String, String>)apiResponse.getData();
            if(!mapCourseIdName.isEmpty()){// key : 동영상(강의)명, value : 출석여부(true:O / false:X)
                List<AttendanceDto> attendanceDtoList = new ArrayList<>();
                AttendanceDto attendanceDto;
                Map<String, Boolean> mapAttendance = new HashMap<>();
                //ApiResponse apiResponse;
                Connection.Response connResTemp = null;
                Document docTemp;
                Elements elemsTemp;
                try {
                    for (String subjectId : mapCourseIdName.keySet()) {
                        //온라인 출석부(출석여부 O/X 체크)
                        apiResponse = doConnectByToken(attendUrl + subjectId, token);
                        if (apiResponse.getData() != null) {
                            connResTemp = (Connection.Response) apiResponse.getData();
                            docTemp = connResTemp.parse();
                            elemsTemp = docTemp.select(".user_progress_table tbody tr");

                            Elements innerElems;
                            String mapKey;
                            String mapValue;
                            for (Element progressEl : elemsTemp) {
                                // 온라인 출석부에서 O, X로 가져올 때 td가 6개면 2번째, td가 4개면 첫번째 컬럼
                                innerElems = progressEl.select("td");
                                mapKey = (innerElems.size() == 6) ? innerElems.get(1).text() : innerElems.first().text();
                                mapValue = (innerElems.size() == 6) ? innerElems.get(4).text() : innerElems.get(3).text();

                                // TODO. 개선 필요
                                if (!mapKey.equals("") && !mapValue.equals("")) {
                                    mapAttendance.put(mapKey, mapValue.equals("O") ? true : false);
                                }
                            }
                        } else {
                            return apiResponse;
                        }

                        // 동영상 목록에서 출석 정보 가져오기
                        apiResponse = doConnectByToken(videoUrl + subjectId, token);
                        if (apiResponse.getData() != null) {
                            connResTemp = (Connection.Response) apiResponse.getData();
                            docTemp = connResTemp.parse();
                            elemsTemp = docTemp.select(".generaltable tbody tr");

                            if (!elemsTemp.isEmpty()) {
                                String videoDetailUrl;
                                String lectureName;
                                for (Element myVideo : elemsTemp) {
                                    videoDetailUrl = myVideo.select(".cell.c1 a").attr("abs:href");
                                    lectureName = myVideo.select(".cell.c1 a").text();

                                    if (!videoDetailUrl.equals("") && !lectureName.equals("")) {
                                        apiResponse = doConnectByToken(videoDetailUrl, token); // todo. 여기가 제일 문제 -> 각 동영상마다 ..
                                        connResTemp = (Connection.Response) apiResponse.getData();
                                        docTemp = connResTemp.parse();

                                        // 진도체크가 설정된 콘텐츠는 기간 전에 학습이 불가
                                        if (!docTemp.select(".alert.alert-info h4 b").text().equals("학습불가")) {
                                            Elements attendTermEls = docTemp.select(".box.vod_info");
                                            attendanceDto = new AttendanceDto();
                                            attendanceDto.setSubjectName(mapCourseIdName.get(subjectId));
                                            attendanceDto.setLectureName(lectureName);
                                            attendanceDto.setIsAttended(mapAttendance.get(lectureName));
                                            attendanceDto.setAttendPageUrl(videoDetailUrl);

                                            for (Element attendTermEl : attendTermEls) {
                                                String attendName = attendTermEl.select("span.vod_info").text(); // 시작일: 또는 출석인정일:이 나와야 함
                                                String attendValue = attendTermEl.select("span.vod_info_value").text();

                                                if (attendName.equals("시작일:"))
                                                    attendanceDto.setAttendedDateTo(attendValue);
                                                if (attendName.equals("출석인정기간:"))
                                                    attendanceDto.setAttendedDateFrom(attendValue);
                                            }
                                            attendanceDtoList.add(attendanceDto);
                                        }
                                    }
                                }
                            }
                        } else {
                            return apiResponse;
                        }
                    }
                    return new ApiResponse(connResTemp.statusCode(), "Success", attendanceDtoList);
                } catch (IOException e) {
                    return new ApiResponse(500, e.getMessage(), null);
                }
            } else {
                return new ApiResponse(404, "수강신청된 과목이 없습니다.", null);
            }
        } else {
            return apiResponse;
        }
    }
    public ApiResponse getAssignments(String token) {
        ApiResponse apiResponse = getCourse(token);

        if(apiResponse.getData() != null) {
            Map<String, String> mapCourseIdName = (Map<String, String>)apiResponse.getData();
            if(!mapCourseIdName.isEmpty()) {
                List<AssignmentDto> listAssignment = new ArrayList<>();
                AssignmentDto assignmentDto;

                Connection.Response connResTemp = null;
                Document docTemp;
                Elements elemsTemp;

                try {
                    for (String subjectId : mapCourseIdName.keySet()) {
                        apiResponse = doConnectByToken(assignUrl + subjectId, token);
                        if(apiResponse.getData() != null) {
                            connResTemp = (Connection.Response) apiResponse.getData();
                            docTemp = connResTemp.parse();

                            if(!docTemp.select(".generaltable tbody").hasClass("empty")) {
                                elemsTemp = docTemp.select(".generaltable tbody tr");
                                // 과제 세부 페이지
                                for(Element assignEl : elemsTemp) {
                                    String detailUrl = assignEl.select("a").attr("abs:href"); // view.php URL 링크 가져옴
                                    if(!detailUrl.equals("")) {
                                        apiResponse = doConnectByToken(detailUrl, token);
                                        if(apiResponse.getData() != null) {
                                            connResTemp = (Connection.Response) apiResponse.getData();
                                            docTemp = connResTemp.parse();
                                            Elements innerElems = docTemp.select(".generaltable tbody tr");

                                            assignmentDto = new AssignmentDto();
                                            assignmentDto.setSubjectName(mapCourseIdName.get(subjectId));
                                            assignmentDto.setAssignName(docTemp.select("#page-content-wrap h2").text());
                                            assignmentDto.setAssignPageUrl(detailUrl);
                                            // 과제 세부정보 테이블
                                            for(Element detailAssignEl : innerElems) {
                                                String colName = detailAssignEl.select("td").first().text();
                                                String colValue = detailAssignEl.select("td").last().text();

                                                if(colName.equals("채점 상황")) {
                                                    // 추후 DTO에 필드 추가 후 set
                                                } else if(colName.equals("종료 일시")) {
                                                    assignmentDto.setAssignedDateTo(colValue);
                                                } else if(colName.equals("최종 수정 일시") && colValue != null) {
                                                    if(colValue.equals("-")) {
                                                        colValue = "";
                                                    }
                                                    assignmentDto.setSubmitDate(colValue);
                                                }
                                                /*else if(colName.equals("마감까지 남은 기한")) {
                                                    // 추후 DTO에 필드 추가 후 set
                                                }*/
                                            }
                                            listAssignment.add(assignmentDto);
                                        } else {
                                            return apiResponse;
                                        }
                                    }
                                }
                            } else {
                                assignmentDto = new AssignmentDto();
                                assignmentDto.setSubjectName(mapCourseIdName.get(subjectId));
                                listAssignment.add(assignmentDto);
                            }
                        } else {
                            return apiResponse;
                        }
                    }
                    return new ApiResponse(connResTemp.statusCode(), "Success", listAssignment);
                } catch(IOException e) {
                    return new ApiResponse(500, e.getMessage(), null);
                }
            } else {
                return new ApiResponse(404, "수강신청된 과목이 없습니다.", null);
            }

        } else {
            return apiResponse;
        }
    }
    /* submitDate == null && quizInfo == null 이면 기간남은 퀴즈 안 푼거 */
    public ApiResponse getQuizzes(String token) {
        ApiResponse apiResponse = getCourse(token);
        if (apiResponse.getData() != null) {
            Map<String, String> mapCourseIdName = (Map<String, String>) apiResponse.getData();

            if(!mapCourseIdName.isEmpty()) {
                List<QuizDto> listQuiz = new ArrayList<>();
                QuizDto quizDto;

                Connection.Response connResTemp = null;
                // index.php (과제명, 마감기한, 제출여부만 알 수 있음)
                Document indexPgDoc;
                Elements indexPgEls;
                // view.php (채점여부, 최종수정일시, 마감까지 남은 기한(n일)도 추가적으로 알 수 있음)
                Document viewPgDoc;
                Elements viewPgEls;

                try {
                    for (String subjectId : mapCourseIdName.keySet()) {
                        apiResponse = doConnectByToken(quizUrl + subjectId, token);
                        if(apiResponse.getData() != null) {
                            connResTemp = (Connection.Response) apiResponse.getData();
                            indexPgDoc = connResTemp.parse();

                            if (!indexPgDoc.select(".generaltable tbody").hasClass("empty")) {
                                indexPgEls = indexPgDoc.select(".generaltable tbody tr");
                                // 퀴즈 세부 페이지
                                for (Element quizEl : indexPgEls) {
                                    String detailUrl = quizEl.select("a").attr("abs:href"); // view.php URL 링크 가져옴
                                    if (!detailUrl.equals("")) {
                                        apiResponse = doConnectByToken(detailUrl, token);
                                        if (apiResponse.getData() != null) {
                                            connResTemp = (Connection.Response) apiResponse.getData();
                                            viewPgDoc = connResTemp.parse();

                                            String quizName = viewPgDoc.select("#page-content-wrap h2").text();
                                            quizDto = new QuizDto();
                                            quizDto.setSubjectName(mapCourseIdName.get(subjectId));
                                            quizDto.setQuizName(quizName);
                                            quizDto.setQuizPageUrl(detailUrl);

                                            // 제출마감기한
                                            viewPgEls = viewPgDoc.select(".box.quizinfo.well");
                                            for (Element quizInfoEl : viewPgEls) {
                                                String checkQuizInfo = quizInfoEl.select("p").text();
                                                if (checkQuizInfo.contains("종료일시")) {
                                                    String strDueDate = checkQuizInfo.split("종료일시 : ")[1];
                                                    quizDto.setQuizDateTo(strDueDate);
                                                }
                                                if (checkQuizInfo.contains("퀴즈를 이용할 수 없음")) {
                                                    quizDto.setQuizInfo("퀴즈를 이용할 수 없음");
                                                }
                                            }

                                            viewPgEls = viewPgDoc.select(".generaltable tbody tr");
                                            // 과제 세부정보 테이블
                                            for (Element detailQuizEl : viewPgEls) {
                                                String checkSubmitInfo = detailQuizEl.select(".lastrow td").first().text();
                                                if (checkSubmitInfo.contains("종료됨")) {
                                                    quizDto.setSubmitDate(checkSubmitInfo.replace("종료됨", "").replace("에 제출됨", ""));
                                                }
                                            }
                                            listQuiz.add(quizDto);
                                        } else {
                                            return apiResponse;
                                        }
                                    }
                                }
                            } else {
                                quizDto = new QuizDto();
                                quizDto.setSubjectName(mapCourseIdName.get(subjectId));
                                listQuiz.add(quizDto);
                            }
                        } else {
                            return apiResponse;
                        }
                    }
                    return new ApiResponse(connResTemp.statusCode(), "Success", listQuiz);
                } catch (IOException e) {
                    return new ApiResponse(500, e.getMessage(), null);
                }
            }
            else {
                return new ApiResponse(404, "수강신청된 과목이 없습니다.", null);
            }
        } else {
            return apiResponse;
        }
    }

    public ApiResponse getCourse(String token) {
        try {
            String url = "https://learn.inha.ac.kr/";
            ApiResponse courseRes = doConnectByToken(url, token);
            if(courseRes.getData() != null) {
                Connection.Response res = (Connection.Response) courseRes.getData();
                Document doc = res.parse();
                //log.info("getCourse doc >> " + doc);
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
                return new ApiResponse(res.statusCode(), "Success", mapCourseIdName);
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

            // 만료된 토큰으로 연결한 url -> https://learn.inha.ac.kr/login.php?errorcode=4
            // 성공 url -> https://learn.inha.ac.kr/
            String resUrl = res.url().toString();
            log.info("conn resUrl >> " + resUrl);
            log.info("conn successUrl >> " + successUrl);
            if(resUrl.contains("errorcode=4")) {
                return new ApiResponse(401, "로그인 세션이 만료되었습니다.", null);
            } else {
                return new ApiResponse(res.statusCode(), "Success", res);
            }
        } catch(IOException e) {
            return new ApiResponse(500, e.getMessage(), null);
        }
    }
}