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
    String attendedDateTo;
    String attendedDateFrom;
    Boolean isAttended;
}
