package com.aloha.time.model;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentDto {
    String subjectName;
    String assignName;
    String submitDate;
    String assignPageUrl;   // 과제 제출할 수 있는 페이지 URL
    String assignedDateFrom;
    String assignedDateTo;  // dueDate에서 변경
    String assignedInfo;    //열리지 않은 퀴즈면, 관련 메시지 보여줌

}
