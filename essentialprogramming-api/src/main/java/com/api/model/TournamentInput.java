package com.api.model;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor

public class TournamentInput {
    @Valid
    private ScheduleInput schedule;

    @NotNull(message = "You need to provide a name for the tournament")
    @NotEmpty(message = "You need to add a name for the tournament")
    @JsonProperty("name")
    private String name;


    @NotNull(message = "You need to specify the registration status for this tournament")
    private boolean registrationOpen;
    private int maxParticipantsNo;
}
