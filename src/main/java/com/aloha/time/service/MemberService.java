package com.aloha.time.service;

import com.aloha.time.model.ApiResponse;
import com.aloha.time.model.ItemDto;
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
        //String successUrl = "https://learn.inha.ac.kr/"; // 로그인 성공 시, URL
        // 로그인 실패 URL -> https://learn.inha.ac.kr/login.php?errorcode=3
        if (successUrl.equals(resUrl)) {
            return new ApiResponse(res.statusCode(), "로그인 성공", token);
        } else if (resUrl.contains("errorcode=3")) {
            return new ApiResponse(401, "아이디 또는 패스워드가 잘못 입력되었습니다.", null);
        } else {
            return new ApiResponse(404, "알 수 없음", null);
        }
    }

    public ApiResponse getAttendances(String token) {
        ApiResponse apiResponse = getCourse(token);

        if (apiResponse.getData() != null) {
            Map<String, String> mapCourseIdName = (Map<String, String>) apiResponse.getData();
            if (!mapCourseIdName.isEmpty()) {// key : 동영상(강의)명, value : 출석여부(true:O / false:X)
                List<ItemDto> listAttendance = new ArrayList<>();
                ItemDto attendance;
                Connection.Response connResTemp = null;
                Document docTemp;
                Elements elemsTemp;
                Document innerDocTemp;
                Elements innerElemsTemp;
                try {
                    String courseId;
                    Map<String, String> mapWeekAttendedStatus;
                    for (String courseLink : mapCourseIdName.keySet()) {
                        courseId = courseLink.split("=")[1];
                        apiResponse = doConnectByToken(courseLink, token); // 과목별 메인페이지
                        if (apiResponse.getData() != null) {
                            connResTemp = (Connection.Response) apiResponse.getData();
                            docTemp = connResTemp.parse();
                            elemsTemp = docTemp.select(".user_attendance_table .attendance_section");
                            //.user_attendance_table .attendance_section .sname 주차 가져옴

                            String strAttend; // 출석 여부 (출석/결석/(공백?)/-)
                            String strWeek; // N주차
                            // key : section-주차(숫자), value : 출석여부
                            mapWeekAttendedStatus = new LinkedHashMap<>(); // 한 과목당, 출석상태 LinkedHashMap는 순서 보장
                            String[] arrText;
                            for (Element attendanceBlock : elemsTemp) {
                                // TODO. text()는 주차 수랑 같이 반환됨 공백으로 잘라서 가져옴 -> 개선 필요
                                arrText = attendanceBlock.text().split(" ");
                                strAttend = arrText[arrText.length - 1];
                                strWeek = attendanceBlock.select("p").attr("data-target");
                                mapWeekAttendedStatus.put("section-" + strWeek, strAttend);
                                if (strAttend.equals("-")) {
                                    break; // 열리지 않은 동영상
                                }

                            }

                            List<String> listNoAttendLecture = new ArrayList<>(); // 결석 동영상 제목
                            // 결석이 있는 경우 온라인 출석부에서 확인
                            if (mapWeekAttendedStatus.values().contains("결석")) {
                                apiResponse = doConnectByToken(attendUrl + courseId, token);

                                if (apiResponse.getData() != null) {
                                    connResTemp = (Connection.Response) apiResponse.getData();
                                    innerDocTemp = connResTemp.parse();
                                    innerElemsTemp = innerDocTemp.select(".user_progress_table tbody tr");

                                    Elements innerElems;
                                    String lectureName;
                                    String attendOx;
                                    // todo. 아래 온라인 출석부 특정 주차만 반복 못하나..
                                    for (Element progressEl : innerElemsTemp) {
                                        // 온라인 출석부에서 O, X로 가져올 때 td가 6개면 2번째, td가 4개면 첫번째 컬럼
                                        innerElems = progressEl.select("td");
                                        lectureName = (innerElems.size() == 6) ? innerElems.get(1).text() : innerElems.first().text();
                                        attendOx = (innerElems.size() == 6) ? innerElems.get(4).text() : innerElems.get(3).text();
                                        log.info("lectureName >> " + lectureName);
                                        log.info("attendOx >> " + attendOx);
                                        if (attendOx.equals("X") && !lectureName.equals("")) {
                                            listNoAttendLecture.add(lectureName);
                                        }
                                    }
                                }
                            }

                            // 출석 세부정보
                            elemsTemp = docTemp.select(".total_sections li.section.main.clearfix .content .section.img-text .activity.vod.modtype_vod .activityinstance");
                            /*log.info("mapWeekAttendedStatus >> " + mapWeekAttendedStatus);
                            log.info("listNoAttendLecture >> " + listNoAttendLecture);*/

                            for (Element weekContent : elemsTemp) {

                                String videoLink = weekContent.select("a").attr("abs:href");
                                if (videoLink.equals("")) {
                                    continue;
                                }
                                Element tempElem;
                                tempElem = weekContent.select(".instancename").first();
                                String lectureName = tempElem != null ? tempElem.ownText() : "";

                                tempElem = weekContent.select(".displayoptions .text-ubstrap").first();
                                String[] arrAttendTerm = tempElem != null ? tempElem.text().split(" ~ ") : new String[2];
                                //log.info("arrAttendTerm >> " + arrAttendTerm[arrAttendTerm.length-1]);

                                attendance = new ItemDto();
                                attendance.setSubjectName(mapCourseIdName.get(courseLink));
                                attendance.setItemName(lectureName);

                                attendance.setIsDone(listNoAttendLecture.contains(lectureName) ? false : true);
                                attendance.setItemLink(videoLink);
                                attendance.setStartDate(arrAttendTerm[0]);
                                attendance.setEndDate(arrAttendTerm[1]);
                                listAttendance.add(attendance);
                            }

                        }
                    }
                    return new ApiResponse(connResTemp.statusCode(), "Success", listAttendance);
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

        if (apiResponse.getData() != null) {
            Map<String, String> mapCourseIdName = (Map<String, String>) apiResponse.getData();
            if (!mapCourseIdName.isEmpty()) {
                List<ItemDto> listAssignment = new ArrayList<>();
                ItemDto assignment;

                Connection.Response connResTemp = null;
                Document docTemp;
                Elements elemsTemp;

                try {
                    String courseId;
                    for (String courseLink : mapCourseIdName.keySet()) {
                        courseId = courseLink.split("=")[1];
                        apiResponse = doConnectByToken(assignUrl + courseId, token);
                        if (apiResponse.getData() != null) {
                            connResTemp = (Connection.Response) apiResponse.getData();
                            docTemp = connResTemp.parse();

                            elemsTemp = docTemp.select(".generaltable tbody tr[class=''], tr[class='lastrow']");
                            //elemsTemp = elemsTemp.select("tr[class=''], tr[class='lastrow']");
                            Elements innerElements;
                            for (Element rowElem : elemsTemp) {
                                //log.info("rowElem >> " + rowElem);

                                assignment = new ItemDto();
                                assignment.setSubjectName(mapCourseIdName.get(courseLink));

                                innerElements = rowElem.select(".cell.c1, .cell.c2, .cell.c3");
                                for (Element cellElem : innerElements) {
                                    if (cellElem.hasClass("c1")) {
                                        assignment.setItemName(cellElem.text());
                                        assignment.setItemLink(cellElem.select("a").attr("abs:href"));
                                    } else if (cellElem.hasClass("c2")) assignment.setEndDate(cellElem.text());
                                    else if (cellElem.hasClass("c3"))
                                        assignment.setIsDone(cellElem.text().equals("제출 완료") ? true : false);
                                    // c1 : 과제명, c2 : endDate, c3 : isDone

                                }
                                listAssignment.add(assignment);
                            }
                        } else {
                            return apiResponse;
                        }
                    }
                    return new ApiResponse(connResTemp.statusCode(), "Success", listAssignment);
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

    public ApiResponse getQuizzes(String token) {
        ApiResponse apiResponse = getCourse(token);
        if (apiResponse.getData() != null) {
            Map<String, String> mapCourseIdName = (Map<String, String>) apiResponse.getData();

            if (!mapCourseIdName.isEmpty()) {
                List<ItemDto> listQuiz = new ArrayList<>();
                ItemDto quiz;

                Connection.Response connResTemp = null;
                Document docTemp;
                Elements elemsTemp;

                try {
                    String courseId;
                    for (String courseLink : mapCourseIdName.keySet()) {
                        courseId = courseLink.split("=")[1];
                        apiResponse = doConnectByToken(quizUrl + courseId, token);

                        Set<String> setLinkTarget = new HashSet<>(); // 성적 값이 없어서 페이지 이동해야 할 타겟
                        if (apiResponse.getData() != null) {
                            connResTemp = (Connection.Response) apiResponse.getData();
                            docTemp = connResTemp.parse();

                            elemsTemp = docTemp.select(".generaltable tbody tr[class=''], tr[class='lastrow']");
                            Elements innerElements;
                            for (Element rowElem : elemsTemp) {
                                quiz = new ItemDto();
                                quiz.setSubjectName(mapCourseIdName.get(courseLink));

                                quiz.setItemName(rowElem.select(".cell.c1").text());
                                quiz.setItemLink(rowElem.select(".cell.c1 a").attr("abs:href"));
                                quiz.setEndDate(rowElem.select(".cell.c2").text());

                                if (rowElem.select(".cell.c3").text().equals("")) {
                                    apiResponse = doConnectByToken(quiz.getItemLink(), token);
                                    if (apiResponse.getData() != null) {
                                        connResTemp = (Connection.Response) apiResponse.getData();
                                        docTemp = connResTemp.parse();
                                        innerElements = docTemp.select(".generaltable.quizattemptsummary .lastrow .cell.c0");
                                        if (!innerElements.isEmpty() && innerElements.first().text().contains("종료됨")) {
                                            quiz.setIsDone(true);
                                        } else {
                                            quiz.setIsDone(false);
                                        }
                                    }
                                } else {
                                    quiz.setIsDone(true);
                                }
                                listQuiz.add(quiz);
                            }
                        } else {
                            return apiResponse;
                        }
                    }
                    return new ApiResponse(connResTemp.statusCode(), "Success", listQuiz);
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

    public ApiResponse getCourse(String token) {
        try {
            String url = "https://learn.inha.ac.kr/";
            ApiResponse courseRes = doConnectByToken(url, token);
            if (courseRes.getData() != null) {
                Connection.Response res = (Connection.Response) courseRes.getData();
                Document doc = res.parse();
                Elements myCourses = doc.select(".course_link");

                //key : 과목 Id, value = 과목명
                Map<String, String> mapCourseIdName = new HashMap<>();
                String courseLink = "";
                String courseName = "";

                for (Element courseEl : myCourses) {
                    courseLink = courseEl.attr("abs:href");
                    courseName = courseEl.select(".course-name .course-title h3").text();
                    mapCourseIdName.put(courseLink, courseName);
                }
                return new ApiResponse(res.statusCode(), "Success", mapCourseIdName);
            } else {
                return courseRes;
            }

        } catch (IOException e) {
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
            if (resUrl.contains("errorcode=4")) {
                return new ApiResponse(401, "로그인 세션이 만료되었습니다.", null);
            } else {
                return new ApiResponse(res.statusCode(), "Success", res);
            }
        } catch (IOException e) {
            return new ApiResponse(500, e.getMessage(), null);
        }
    }

}