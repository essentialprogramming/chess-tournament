package com.api.output;


import lombok.*;

import java.io.Serializable;
import java.util.List;

@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor

public class TournamentJSON implements Serializable {

    private String tournamentKey;
    private ScheduleJSON schedule;
    private String name;
    private boolean registrationOpen;
    private int maxParticipantsNo;
    private List<UserJSON> referees;
}
