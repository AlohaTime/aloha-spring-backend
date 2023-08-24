package com.aloha.time.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class AttendanceDto {
    Integer month;
    Integer week;
    String subjectName;
    String attendedDate;
    String attendedDateTo;
    String attendedDateFrom;
    Boolean isAttended;
}
