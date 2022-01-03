package com.api.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class TournamentUserKey implements Serializable {

    @Column(name = "tournament_id")
    private int tournamentId;

    @Column(name = "user_id")
    private int userId;

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj == null || getClass() != obj.getClass()) return false;
        TournamentUserKey that = (TournamentUserKey) obj;
        return tournamentId == that.tournamentId &&
                userId == that.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tournamentId, userId);
    }
}
