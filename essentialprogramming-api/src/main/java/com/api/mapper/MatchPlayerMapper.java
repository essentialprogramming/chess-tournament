package com.api.mapper;

import com.api.entities.MatchPlayer;
import com.api.output.MatchPlayerJSON;

public class MatchPlayerMapper {

    public static MatchPlayerJSON entityToJSON(MatchPlayer matchPlayer) {
        return MatchPlayerJSON.builder()
                .firstPlayer(PlayerMapper.playerToJson(matchPlayer.getFirstPlayer()))
                .secondPlayer(PlayerMapper.playerToJson(matchPlayer.getSecondPlayer()))
                .matchKey(matchPlayer.getMatch().getMatchKey())
                .build();
    }
}
