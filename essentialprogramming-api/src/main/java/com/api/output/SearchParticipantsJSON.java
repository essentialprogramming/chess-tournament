package com.api.output;

import com.api.model.InvitationStatus;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchParticipantsJSON {

    private String playerKey;
    private String email;
    private String firstName;
    private String lastName;
    private Double score;
    private String invitationStatus;
    private int tournamentsWon;
    private Long totalMatchesPlayed;

    public SearchParticipantsJSON(String playerKey, String email, String firstName, String lastName, Double score, Long totalMatchesPlayed, String invitationStatus) {
        this.playerKey = playerKey;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.score = score;
        this.invitationStatus = invitationStatus;
        this.totalMatchesPlayed = totalMatchesPlayed;
    }
}
