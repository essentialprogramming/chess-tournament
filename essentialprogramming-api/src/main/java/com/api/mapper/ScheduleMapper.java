package com.api.mapper;

import com.api.entities.Schedule;
import com.api.model.ScheduleInput;
import com.api.output.ScheduleJSON;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class ScheduleMapper {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static Schedule inputToSchedule(ScheduleInput input){
        return Schedule.builder()
                .startDate(LocalDateTime.parse(input.getStartDate(),formatter))
                .endDate(LocalDateTime.parse(input.getEndDate(),formatter))
                .location(input.getLocation())
                .build();
    }

    public static ScheduleJSON scheduleToJSON(Schedule schedule){
        return ScheduleJSON.builder()
                .startDate(schedule.getStartDate().format(formatter))
                .endDate(schedule.getEndDate().format(formatter))
                .location(schedule.getLocation())
                .build();
    }
}
