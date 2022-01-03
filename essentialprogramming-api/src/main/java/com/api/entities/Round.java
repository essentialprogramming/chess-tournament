package com.api.entities;

import com.api.model.GameState;
import lombok.*;

import javax.persistence.*;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "round")
public class Round {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private int id;

    @Column(name = "number", nullable = false)
    private int number;

    @Column(name = "round_key")
    private String roundKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "state")
    private GameState state;

    @ManyToOne
    @JoinColumn(name = "tournament_id", referencedColumnName = "id", nullable = false)
    private Tournament tournament;

    @OneToMany(mappedBy = "round", fetch = FetchType.EAGER)
    private List<Match> matches;
}
