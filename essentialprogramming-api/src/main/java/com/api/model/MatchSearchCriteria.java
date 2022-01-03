package com.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Getter
@Setter
public class MatchSearchCriteria {

    @Schema(example = "Avangarde Chess Tournament 2021")
    @Size(min = 3, message = "Tournament name criteria must be at least 3 characters long!")
    private String tournamentName;

    @Schema(example = "9")
    @Min(value = 1, message = "Round number criteria must be an integer higher than 0!")
    private Integer roundNo;

    @Schema(example = "ACTIVE")
    @Pattern(regexp = Patterns.GAME_STATE_REGEXP,
            message = "Match state criteria must be one of the following: 'CREATED', 'ACTIVE' or 'ENDED'!")
    private String matchState;

    @Schema(example = "FIRST")
    private String result;

    @Schema(example = "2021-09-20 12:30")
    @Pattern(regexp = Patterns.YYYY_MM_DD_HH_MM_REGEXP,
            message = "Date field must match the following pattern: 'yyy-mm-dd hh:MM'")
    private String startDateMin;

    @Schema(example = "2021-09-20 12:30")
    @Pattern(regexp = Patterns.YYYY_MM_DD_HH_MM_REGEXP,
            message = "Date field must match the following pattern: 'yyy-mm-dd hh:MM'")
    private String startDateMax;
    
    @Size(min = 3, message = "Referee name criteria must be at least 3 characters long!")
    private String referee;


    @JsonIgnore
    public boolean isEmpty() {
        return tournamentName == null
                && roundNo == null
                && matchState == null
                && result == null
                && startDateMin == null
                && startDateMax == null
                && referee == null;
    }
}
