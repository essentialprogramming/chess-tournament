package com.api.output;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleJSON implements Serializable {

    private String startDate;
    private String endDate;
    private String location;

    @Override
    public String toString() {
        return "  Start date: " + startDate + "\n" +
                "  End date: " + endDate + "\n" +
                "  Location: " + location + "\n";

    }
}
