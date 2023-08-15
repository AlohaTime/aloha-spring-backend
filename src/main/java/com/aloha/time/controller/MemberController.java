package com.aloha.time.controller;

import com.aloha.time.entity.Member;
import com.aloha.time.model.MemberCreateRequest;
import com.aloha.time.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {
    private final MemberService memberService;

    @GetMapping
    public ResponseEntity<?> getUser(@RequestParam(required = false) Long id,
                                     @RequestParam String email) {
        List<Member> memberList = memberService.getUser(id, email);
        return ResponseEntity.ok(memberList);
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody MemberCreateRequest request) {
        String msg = memberService.createUser(request.getPassword(), request.getEmail());
        return ResponseEntity.ok(msg);
    }

}