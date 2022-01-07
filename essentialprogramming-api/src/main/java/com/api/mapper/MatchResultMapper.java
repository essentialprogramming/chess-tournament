package com.api.mapper;

import com.api.entities.MatchResult;
import com.api.output.MatchResultJSON;

public class MatchResultMapper {

    public static MatchResultJSON entityToJSON(MatchResult matchResult) {
        MatchResultJSON matchResultJSON = MatchResultJSON.builder()
                .key(matchResult.getMatchResultKey())
                .firstPlayer(PlayerMapper.playerToJson(matchResult.getFirstPlayer()))
                .secondPlayer(PlayerMapper.playerToJson(matchResult.getSecondPlayer()))
                .build();


        if (matchResult.getFirstPlayerResult() != null) {
            matchResultJSON.setFirstPlayerResult(matchResult.getFirstPlayerResult().getValue());
        }
        if (matchResult.getSecondPlayerResult() != null) {
            matchResultJSON.setSecondPlayerResult(matchResult.getSecondPlayerResult().getValue());
        }
        if (matchResult.getResult() != null) {
            matchResultJSON.setFinalResult(String.valueOf(matchResult.getResult().getValue()));
        }
        return matchResultJSON;
    }
}

