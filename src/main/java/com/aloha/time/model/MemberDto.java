package com.aloha.time.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberDto {
    long id;
    String password;
    String email;
}
