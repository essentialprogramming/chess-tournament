package com.api.repository;

import com.api.entities.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TournamentUserRepository extends JpaRepository<TournamentUser, TournamentUserKey> {

    List<TournamentUser> findByTournamentOrderByScoreDesc(Tournament tournament);

    Optional<TournamentUser> findTopByTournamentOrderByScoreDesc(Tournament tournament);

    List<TournamentUser> findUserByTournament(Tournament tournament);

    void deleteAllByTournament(Tournament tournament);

    int countTournamentUserByTournament(Tournament tournament);
}
