package com.aloha.time.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class AttendanceDto {
    /*Integer month;
    Integer week;
    String attendedDate;*/
    String subjectName;
    String lectureName;
    String attendedDateTo;
    String attendedDateFrom;
    Boolean isAttended;
    String errorMsg; // 결과값이 없는 경우, 반환할 메시지

}
