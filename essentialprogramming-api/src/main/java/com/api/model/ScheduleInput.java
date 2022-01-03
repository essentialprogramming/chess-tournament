package com.api.model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor

public class ScheduleInput {

    @Schema(name = "startDate", description = "The start date of the tournament", example = "2021-09-20 12:30")
    @Pattern(regexp = Patterns.YYYY_MM_DD_HH_MM_REGEXP)
    private String startDate;

    @Schema(name = "endDate", description = "The end date of the tournament", example = "2021-10-10 17:50")
    @Pattern(regexp = Patterns.YYYY_MM_DD_HH_MM_REGEXP)
    private String endDate;

    private String location;
}
