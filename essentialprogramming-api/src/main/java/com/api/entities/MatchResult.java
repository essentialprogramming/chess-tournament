package com.api.entities;

import com.api.model.Result;
import lombok.*;

import javax.persistence.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "match_result")
public class MatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private int id;

    @Column(name = "match_result_key")
    private String matchResultKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false)
    private Result result;

    @Enumerated(EnumType.STRING)
    @Column(name = "first_player_result")
    private Result firstPlayerResult;

    @Enumerated(EnumType.STRING)
    @Column(name = "second_player_result")
    private Result secondPlayerResult;

    @OneToOne
    @JoinColumn(name = "first_player_id", referencedColumnName = "id", nullable = false)
    private Player firstPlayer;

    @OneToOne
    @JoinColumn(name = "second_player_id", referencedColumnName = "id", nullable = false)
    private Player secondPlayer;

}
