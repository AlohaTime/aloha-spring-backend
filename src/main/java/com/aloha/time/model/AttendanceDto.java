package com.aloha.time.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceDto {
    String subjectName;
    String lectureName;
    Boolean isAttended;
    String attendedDateTo;
    String attendedDateFrom;
    String attendPageUrl; // 출석할 수 있도록 동영상 페이지 URL
    String attendInfo;    //열리지 않은 퀴즈면, 관련 메시지 보여줌

}
