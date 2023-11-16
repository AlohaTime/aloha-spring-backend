package com.aloha.time.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {
    int code; //200
    String message; //success
    Object data; //
}
