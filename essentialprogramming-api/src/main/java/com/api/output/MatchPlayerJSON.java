package com.api.output;

import lombok.*;

import java.io.Serializable;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MatchPlayerJSON implements Serializable {
    private String matchKey;
    private PlayerJSON firstPlayer;
    private PlayerJSON secondPlayer;
    private String tournamentKey;
}
