package com.api.mapper;

import com.api.entities.Match;
import com.api.entities.Round;
import com.api.output.RoundJSON;

import java.util.stream.Collectors;

public class RoundMapper {
    public static RoundJSON entityToJSON(Round round) {
        return RoundJSON.builder()
                .roundKey(round.getRoundKey())
                .number(round.getNumber())
                .matches(round.getMatches().stream()
                        .filter(RoundMapper::hasNoGhostPlayer)
                        .map(MatchMapper::entityToJSON)
                        .collect(Collectors.toList())
                )
                .tournamentKey(round.getTournament().getTournamentKey())
                .build();

    }

    private static boolean hasNoGhostPlayer(Match match) {
        return !match.getMatchResult().getFirstPlayer().getEmail().contains("GHOST_EMAIL_")
                && !match.getMatchResult().getSecondPlayer().getEmail().contains("GHOST_EMAIL_");
    }
}
