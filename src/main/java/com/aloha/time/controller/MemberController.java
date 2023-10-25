package com.aloha.time.controller;

import com.aloha.time.model.*;
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
    public ApiResponse login(@RequestBody LoginRequest request) {
        return memberService.loginUser(request.getId(), request.getPassword());
    }

    // API 규격은 memberId로 출석 여부를 가져온다고 했지만, 로그인한 토큰만 있으면 되는 듯?
    @GetMapping("/attendances")
    public ApiResponse getAttendances(String token) throws IOException, ParseException {
        return memberService.getAttendances(token);
    }
    @GetMapping("/assignments")
    public ApiResponse getAssignments(String token) throws IOException {
        return memberService.getAssignments(token);
    }
    /*@GetMapping
    public ResponseEntity<?> getUser(Long id, @RequestParam String email) {
        List<Member> memberList = memberService.getUser(id, email);
        return ResponseEntity.ok(memberList);
    }*/

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody MemberCreateRequest request) {
        String msg = memberService.createUser(request.getPassword(), request.getEmail());
        return ResponseEntity.ok(msg);
    }



}