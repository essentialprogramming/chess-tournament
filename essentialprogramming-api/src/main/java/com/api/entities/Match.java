package com.api.entities;

import com.api.model.GameState;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "match")
@Table(name = "match")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private GameState state;

    @Column(name = "match_key")
    private String matchKey;

    @Column(name="start_date")
    private LocalDateTime startDate;

    @ManyToOne
    @JoinColumn(name = "round_id", referencedColumnName = "id", nullable = false)
    private Round round;

    @OneToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "match_result_id", referencedColumnName = "id")
    private MatchResult matchResult;

    @ManyToOne
    @JoinColumn(name = "tournament_id", referencedColumnName = "id", nullable = false)
    private Tournament tournament;

    @ManyToOne
    @JoinColumn(name = "referee_id", referencedColumnName = "id")
    private User referee;
}
