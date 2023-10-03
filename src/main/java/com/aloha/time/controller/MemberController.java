package com.aloha.time.controller;

import com.aloha.time.model.AssignmentDto;
import com.aloha.time.model.AttendanceDto;
import com.aloha.time.model.LoginRequest;
import com.aloha.time.model.MemberCreateRequest;
import com.aloha.time.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/members")
public class MemberController {
    private final MemberService memberService;

    // 로그인이나 출석정보AttendanceGetRequest
    @PostMapping("/login")
    public String login(@RequestBody LoginRequest request) throws IOException {
        String token = memberService.loginUser(request.getId(), request.getPassword());
        return token;
    }

    // API 규격은 memberId로 출석 여부를 가져온다고 했지만, 로그인한 토큰만 있으면 되는 듯?
    @GetMapping("/attendances")
    public List<AttendanceDto> getAttendances(@RequestParam(required = true) String token) throws IOException, ParseException {
        return memberService.getAttendances(token);
    }
    @GetMapping("/assignments")
    public List<AssignmentDto> getAssignments(@RequestParam(required = true) String token) throws IOException {
        return memberService.getAssignments(token);
    }

}