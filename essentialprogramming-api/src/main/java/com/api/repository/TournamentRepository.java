package com.api.repository;

import com.api.entities.Tournament;
import com.api.model.GameState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament,Integer>, TournamentRepositoryCustom {

    Optional<Tournament> findByName(String name);

    Optional<Tournament> findByTournamentKey(String tournamentKey);

    @Query("SELECT distinct new com.api.entities.Tournament(t.schedule, t.name, t.registrationOpen, t.maxParticipants, t.state, t.tournamentKey) "+
            "FROM Tournament t WHERE t.state = :state")
    List<Tournament> findAllByState(@Param("state") GameState state);

    @Override
    @Query("SELECT distinct new com.api.entities.Tournament(t.schedule, t.name, t.registrationOpen, t.maxParticipants, t.state, t.tournamentKey) "+
           "FROM Tournament t ORDER BY t.state")
    List<Tournament> findAll();
}
