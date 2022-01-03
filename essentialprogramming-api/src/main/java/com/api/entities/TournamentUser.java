package com.api.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tournament_user")
public class TournamentUser {

    @EmbeddedId
    private TournamentUserKey tournamentUserId;

    @ManyToOne
    @MapsId("tournamentId")
    @JoinColumn(name = "tournament_id", referencedColumnName = "id", nullable = false)
    private Tournament tournament;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @Column(name = "score")
    private double score;

    public TournamentUser(Tournament tournament, User user) {
        this.tournament = tournament;
        this.user = user;
        this.score = 0;
        this.tournamentUserId = new TournamentUserKey(tournament.getId(), user.getId());
    }
}
