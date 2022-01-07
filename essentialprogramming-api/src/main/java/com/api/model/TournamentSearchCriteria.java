package com.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Pattern;

@Getter
@Setter
public class TournamentSearchCriteria {
    private String name;

    @Schema(example = "2021-10-15 10:30")
    @Pattern(regexp = Patterns.YYYY_MM_DD_HH_MM_REGEXP)
    private String startDate;

    @Schema(example = "2021-10-25 17:45")
    @Pattern(regexp = Patterns.YYYY_MM_DD_HH_MM_REGEXP)
    private String endDate;

    @JsonIgnore
    public boolean isEmpty() {
        return name == null
                && startDate == null
                && endDate == null;
    }
}
