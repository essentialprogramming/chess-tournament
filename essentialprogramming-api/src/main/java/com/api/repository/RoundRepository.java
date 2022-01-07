package com.api.repository;

import com.api.entities.Round;
import com.api.entities.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoundRepository extends JpaRepository<Round, Integer> {

    List<Round> findAllByTournament(Tournament tournament);

    Optional<Round> findByRoundKey(String roundKey);

    void deleteAllByTournament(Tournament tournament);
}
