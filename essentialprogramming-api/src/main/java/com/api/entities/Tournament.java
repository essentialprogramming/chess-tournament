package com.api.entities;

import com.api.model.GameState;
import lombok.*;

import javax.persistence.*;
import java.util.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tournament")
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private int id;

    @Column(name = "tournament_key")
    private String tournamentKey;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "registration_open", nullable = false)
    private boolean registrationOpen;

    @Column(name = "max_participants_no", nullable = false)
    private int maxParticipants;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private GameState state;

    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "schedule_id", referencedColumnName = "id", nullable = false)
    private Schedule schedule;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.PERSIST)
    @MapKeyColumn(name = "number")
    private Map<Integer, Round> rounds;

    @OneToOne
    @JoinColumn(name = "current_round_id", referencedColumnName = "id")
    private Round currentRound;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private List<MatchPlayer> matchPlayerPairs;

    @ManyToMany
    @JoinTable(name = "tournament_user",
    joinColumns = { @JoinColumn(name = "tournament_id", referencedColumnName = "id")},
    inverseJoinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "id")})
    private List<Player> players = new ArrayList<>();

    @OneToMany(mappedBy = "tournament")
    private List<TournamentUser> tournamentUsers;

    @ManyToMany(mappedBy = "tournaments")
    private Set<User> referees = new HashSet<>();

    public boolean addPlayer(Player player) {
        if (!players.contains(player)) {
            this.players.add(player);
            player.playerTournaments.add(this);

            return true;
        }

        return false;
    }

    public boolean addReferee(User referee) {
        if (!referees.contains(referee)) {
            this.referees.add(referee);
            if (referee.getTournaments() == null) {
                referee.setTournaments(new HashSet<>());
            }
            referee.getTournaments().add(this);
            return true;
        }
        return false;
    }
    public Tournament(Schedule schedule, String name, boolean registrationOpen, int maxParticipantsNo, GameState state, String tournamentKey) {
        this.schedule = schedule;
        this.name = name;
        this.registrationOpen = registrationOpen;
        this.maxParticipants = maxParticipantsNo;
        this.state = state;
        this.tournamentKey = tournamentKey;
    }

}
