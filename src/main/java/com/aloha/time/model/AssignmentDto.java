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

    String dueDate;
    //Boolean isSubmit; // 사용안함
    String errorMsg; // 결과값이 없는 경우, 반환할 메시지
}
