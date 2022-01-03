package com.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Getter
@Setter
public class ParticipantSearchCriteria {

    private String name;
    private String email;
    private Double score;
    private Double minScore;
    private Double maxScore;
    private InvitationStatus invitationStatus;

    @Schema(example = "firstName")
    private String sortKey;

    @Pattern(regexp = Patterns.ASC_DESC_REGEXP, message = "Sorting order must be either 'asc' or 'desc'")
    @Schema(example = "asc")
    private String sortOrder;

    @Schema(example = "2021-09-20 12:30")
    @Pattern(regexp = Patterns.YYYY_MM_DD_HH_MM_REGEXP,
            message = "Date field must match the following pattern: 'yyy-mm-dd hh:MM'")
    private String createdDateStart;

    @Schema(example = "2021-09-20 12:30")
    @Pattern(regexp = Patterns.YYYY_MM_DD_HH_MM_REGEXP,
            message = "Date field must match the following pattern: 'yyy-mm-dd hh:MM'")
    private String createdDateEnd;

    @Size(min = 3, message = "Quick search criteria must have at least 3 characters!")
    private String quickSearch;

    @JsonIgnore
    public boolean isEmpty() {
        return  email == null
                && name == null
                && score == null
                && createdDateStart == null
                && createdDateEnd == null
                && minScore == null
                && maxScore == null
                && quickSearch == null
                && invitationStatus == null
                && sortKey == null
                && sortOrder == null;
    }
}
