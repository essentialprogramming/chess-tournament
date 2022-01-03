package com.api.repository;

import com.api.entities.Player;
import com.api.entities.Tournament;
import com.api.entities.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, Integer> {
    @Query("FROM UserSettings us WHERE us.tournament = ?1 AND us.active = true")
    List<UserSettings> findByTournament(Tournament tournament);

    @Query("FROM UserSettings us WHERE us.player = ?1 AND us.tournament = ?2 AND us.active = true")
    Optional<UserSettings> findByPlayerAndTournament(Player player, Tournament tournament);

    @Transactional
    @Modifying
    @Query("DELETE FROM UserSettings us WHERE us.player = ?1 and us.tournament = ?2")
    void deleteAllByPlayerAndTournament(Player player, Tournament tournament);

    @Transactional
    @Modifying
    @Query("DELETE FROM UserSettings us WHERE us.tournament = ?1")
    void deleteAllByTournament(Tournament tournament);

    @Query("SELECT count(us) FROM UserSettings us WHERE us.player = ?1 AND us.tournament = ?2 AND us.active = true")
    int countExistingAndActive(Player player, Tournament tournament);
}