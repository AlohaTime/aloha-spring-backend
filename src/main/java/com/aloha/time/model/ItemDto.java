package com.aloha.time.model;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemDto {
    String subjectName;
    String itemName;
    Boolean isDone;
    String itemLink;
    String startDate;
    String endDate;

}
