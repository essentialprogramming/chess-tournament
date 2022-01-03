package com.api.repository;

import com.api.entities.Match;
import com.api.entities.Player;
import com.api.entities.Tournament;
import com.api.entities.User;
import com.api.model.GameState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Integer>, JpaSpecificationExecutor<Match>, MatchRepositoryCustom {
    Optional<Match> findByMatchKey(String matchKey);

    Optional<Match> findTopByTournamentAndStateOrderByStartDateDesc(Tournament tournament, GameState state);

    long countByTournamentAndState(Tournament tournament, GameState state);

    void deleteAllByTournament(Tournament tournament);

    long countByReferee(User user);
}
