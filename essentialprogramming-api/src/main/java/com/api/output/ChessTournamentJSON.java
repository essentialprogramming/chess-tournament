package com.api.output;

import lombok.*;

import java.io.Serializable;
import java.util.Map;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChessTournamentJSON implements Serializable {

    private String tournamentName;
    private String tournamentKey;
    private ScheduleJSON schedule;
    private Map<Integer,RoundJSON> rounds;

    @Override
    public String toString() {
        return "Tournament name: " + tournamentName + "\n" +
                "Tournament key: " + tournamentKey + "\n"+ "\n" +
                "Schedule:" + "\n" + "{" +"\n"+ schedule + "}" + "\n" + "\n" +
                "Rounds: " + "\n" + rounds + "\n" + "\n";
    }

}
