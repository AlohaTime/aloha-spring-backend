package com.aloha.time.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceDto {
    String subjectName;
    String lectureName;
    String attendedDateTo;
    String attendedDateFrom;
    Boolean isAttended;
    String videoPageUrl; // 출석할 수 있도록 동영상 페이지 URL
    String errorMsg; // 결과값이 없는 경우, 반환할 메시지

}
