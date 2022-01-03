package com.api.repository;

import com.api.entities.MatchPlayer;
import com.api.entities.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchPlayerRepository extends JpaRepository <MatchPlayer, Integer> {
    List<MatchPlayer> findAllByTournament(Tournament tournament);

    void deleteAllByTournament(Tournament tournament);
}
