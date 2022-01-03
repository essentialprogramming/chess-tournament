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
public class MatchPlayerKey implements Serializable {

    @Column(name = "match_id")
    private int matchId;

    @Column(name = "first_player_id")
    private int firstPlayerId;

    @Column(name = "second_player_id")
    private int secondPlayerId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MatchPlayerKey that = (MatchPlayerKey) o;
        return matchId == that.matchId && firstPlayerId == that.firstPlayerId && secondPlayerId == that.secondPlayerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(matchId, firstPlayerId, secondPlayerId);
    }
}
