package com.api.repository;

import com.api.entities.Player;
import com.api.output.PlayerJSON;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository

public interface PlayerRepository extends JpaRepository<Player, Integer>, JpaSpecificationExecutor<Player>, PlayerRepositoryCustom, RefereeRepositoryCustom {

    @Query("SELECT distinct new com.api.output.PlayerJSON(p.email, p.firstName, p.lastName, p.score, p.userKey) " +
            "FROM Player p WHERE p.email NOT LIKE 'GHOST_EMAIL_%' ORDER BY p.score DESC")
    List<PlayerJSON> findAllByOrderByScoreDesc();

    Optional<Player> findById(int id);

    Optional<Player> findByUserKey(String userKey);

    Optional<Player> findByEmail(String email);

    void deleteByEmail(String email);

    void deletePlayerByUserKeyIn(List<String> keys);
}