package com.aloha.time.model;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizDto {
    String subjectName;
    String quizName;
    String submitDate;
    String solvePageUrl; // 퀴즈풀 수 있는 페이지 URL
    String dueDate;
    String quizInfo; //열리지 않은 퀴즈면, 관련 메시지 보여줌
}
