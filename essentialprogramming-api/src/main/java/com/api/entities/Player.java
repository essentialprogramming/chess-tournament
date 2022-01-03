package com.api.entities;

import lombok.*;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import java.util.List;

@SuperBuilder
@Getter
@Setter
@Entity
@DiscriminatorValue("player")
public class Player extends User{

    public Player() {
        super();
    }

    @Column(name = "score")
    private double score;

    @ManyToMany(mappedBy = "players")
    List<Tournament> playerTournaments;

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL)
    private List<UserSettings> userSettings;

}
