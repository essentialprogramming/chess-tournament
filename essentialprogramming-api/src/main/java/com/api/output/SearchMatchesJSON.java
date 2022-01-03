package com.api.output;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchMatchesJSON {

    private int matchNumber;
    private String tournamentName;
    private int roundNo;
    private String matchResult;
    private String matchState;
    private String startDate;
    private String refereeEmail;
    private String winner;


    public SearchMatchesJSON(String matchState, int roundNo, String tournamentName, String refereeEmail, String matchResult, String winner) {
        this.matchState = matchState;
        this.roundNo = roundNo;
        this.tournamentName = tournamentName;
        this.refereeEmail = refereeEmail;
        this.matchResult = matchResult;
        this.winner = winner;
    }
}
