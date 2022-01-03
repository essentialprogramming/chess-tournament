package com.api.output;

import lombok.*;

@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SearchTournamentsJSON {

    private String name;
    private String encryptedTournamentKey;
    private int maxParticipantsNo;
    private boolean registrationOpen;
    private String winner;
}
