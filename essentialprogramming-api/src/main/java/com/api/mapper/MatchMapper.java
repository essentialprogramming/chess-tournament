package com.api.mapper;

import com.api.entities.Match;
import com.api.entities.MatchResult;
import com.api.model.Result;
import com.api.output.MatchJSON;
import com.api.output.SearchMatchesJSON;
import com.util.date.DateUtil;
import lombok.SneakyThrows;

import java.util.Optional;

public class MatchMapper {

    public static MatchJSON entityToJSON(Match match) {
        return MatchJSON.builder()
                .state(match.getState().toString())
                .roundId(match.getRound().getId())
                .matchKey(match.getMatchKey())
                .tournamentKey(match.getTournament().getTournamentKey())
                .matchKey(match.getMatchKey())
                .startDate(match.getStartDate() != null ? match.getStartDate().toString() : null)
                .matchResultKey(match.getMatchResult() != null ? match.getMatchResult().getMatchResultKey() : null)
                .matchResult(Optional.ofNullable(match)
                        .map(Match::getMatchResult)
                        .map(MatchResult::getResult)
                        .map(Result::toString)
                        .orElse(null))
                .result(match.getMatchResult() != null ? MatchResultMapper.entityToJSON(match.getMatchResult()) : null)
                .referee(match.getReferee() == null ? null : UserMapper.userToJson(match.getReferee()))
                .build();
    }

    @SneakyThrows
    public static SearchMatchesJSON entityToSearchMatchJSON(Match match){
        return SearchMatchesJSON.builder()
                .matchState(match.getState().name())
                .startDate((match.getStartDate() != null ? DateUtil.format(match.getStartDate(), "yyyy-MM-dd HH:mm") : null))
                .tournamentName(match.getTournament().getName())
                .roundNo(match.getRound().getNumber())
                .refereeEmail(match.getReferee() != null ? match.getReferee().getEmail() : "No referee assigned")
                .matchResult(match.getMatchResult().getResult() != null ? match.getMatchResult().getResult().name() : "No result")
                .build();
    }
}
