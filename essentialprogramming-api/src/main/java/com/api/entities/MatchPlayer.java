package com.api.entities;

import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "match_player")
public class MatchPlayer {

    @EmbeddedId
    private MatchPlayerKey matchPlayerId;

    @Column(name = "match_player_key")
    private String matchPlayerKey;

    @OneToOne(cascade = CascadeType.PERSIST)
    @MapsId("matchId")
    @JoinColumn(name = "match_id", referencedColumnName = "id", nullable = false)
    private Match match;

    @OneToOne
    @MapsId("firstPlayerId")
    @JoinColumn(name = "first_player_id", referencedColumnName = "id", nullable = false)
    private Player firstPlayer;

    @OneToOne
    @MapsId("secondPlayerId")
    @JoinColumn(name = "second_player_id", referencedColumnName = "id", nullable = false)
    private Player secondPlayer;

    @ManyToOne
    @JoinColumn(name = "tournament_id", referencedColumnName = "id", nullable = false)
    private Tournament tournament;

    public MatchPlayer(Match match, Player firstPlayer, Player secondPlayer, Tournament tournament) {
        this.match = match;
        this.firstPlayer = firstPlayer;
        this.secondPlayer = secondPlayer;
        this.tournament = tournament;
        this.matchPlayerId = new MatchPlayerKey(match.getId(), firstPlayer.getId(), secondPlayer.getId());
    }
}
