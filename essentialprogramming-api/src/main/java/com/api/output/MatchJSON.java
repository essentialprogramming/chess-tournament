package com.api.output;

import lombok.*;

import java.io.Serializable;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MatchJSON implements Serializable {

    private String tournamentKey;
    private int roundId;
    private String matchKey;
    private String state;
    private String matchResultKey;
    private String startDate;
    private MatchResultJSON result;
    private String matchResult;
    private UserJSON referee;

    @Override
    public String toString() {
        return "          State: " + state + "\n"+
                "           Round id: " + roundId + "\n" +
                "           Match result: " + result + "\n" +
                "           The winner: " + matchResult + "\n" + "\n" ;
    }
}
