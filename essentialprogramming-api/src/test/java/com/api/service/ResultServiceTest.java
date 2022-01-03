package com.api.service;

import com.api.entities.*;
import com.api.model.GameState;
import com.api.model.Result;
import com.api.repository.PlayerRepository;
import com.api.repository.TournamentUserRepository;
import com.util.TestEntityGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ResultServiceTest {

    @InjectMocks
    private ResultService resultService;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private TournamentUserRepository tournamentUserRepository;

    private Match match;

    @BeforeEach
    public void createMatch() {

        match = TestEntityGenerator.generateMatch(GameState.CREATED);
        Round round = TestEntityGenerator.generateRound(GameState.CREATED);
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.CREATED, true, 10);
        MatchResult matchResult = TestEntityGenerator.generateMatchResult(Result.FIRST);
        Player firstPlayer = TestEntityGenerator.generatePlayer(false, true, true);
        Player secondPlayer = TestEntityGenerator.generatePlayer(false, true, true);
        match.setRound(round);
        match.setTournament(tournament);
        match.setMatchResult(matchResult);
        matchResult.setFirstPlayer(firstPlayer);
        matchResult.setSecondPlayer(secondPlayer);
    }

    @Test
    public void testApplyResultsSuccessfullyResultDraw() {

        //given
        TournamentUser firstPlayerInTournament = TestEntityGenerator.generateTournamentUser(match.getMatchResult().getFirstPlayer(), match.getTournament());
        TournamentUser secondPlayerInTournament = TestEntityGenerator.generateTournamentUser(match.getMatchResult().getSecondPlayer(), match.getTournament());

        double firstPlayerScoreBeforeResult = match.getMatchResult().getFirstPlayer().getScore();
        double secondPlayerScoreBeforeResult = match.getMatchResult().getSecondPlayer().getScore();

        double firstPlayerScoreInTournament = firstPlayerInTournament.getScore();
        double secondPlayerScoreInTournament = secondPlayerInTournament.getScore();

        when(playerRepository.findById(match.getMatchResult().getFirstPlayer().getId()))
                .thenReturn(Optional.of(match.getMatchResult().getFirstPlayer()));
        when(playerRepository.findById(match.getMatchResult().getSecondPlayer().getId())).
                thenReturn(Optional.of(match.getMatchResult().getSecondPlayer()));
        when(tournamentUserRepository.findById(new TournamentUserKey(match.getTournament().getId(), match.getMatchResult().getFirstPlayer().getId())))
                .thenReturn(Optional.of(firstPlayerInTournament));
        when(tournamentUserRepository.findById(new TournamentUserKey(match.getTournament().getId(), match.getMatchResult().getSecondPlayer().getId())))
                .thenReturn(Optional.of(secondPlayerInTournament));

        //when
        resultService.applyResults(firstPlayerInTournament.getUser().getId(),
                secondPlayerInTournament.getUser().getId(),
                Result.DRAW,
                firstPlayerInTournament.getTournament().getId());

        //then
        assertThat(match.getMatchResult().getFirstPlayer().getScore()).isEqualTo(firstPlayerScoreBeforeResult + 0.5);
        assertThat(match.getMatchResult().getSecondPlayer().getScore()).isEqualTo(secondPlayerScoreBeforeResult + 0.5);
        assertThat(firstPlayerInTournament.getScore()).isEqualTo(firstPlayerScoreInTournament + 0.5);
        assertThat(secondPlayerInTournament.getScore()).isEqualTo(secondPlayerScoreInTournament + 0.5);
    }

    @Test
    public void testApplyResultsSuccessfullyResultFirst() {

        //given
        TournamentUser firstPlayerInTournament = TestEntityGenerator.generateTournamentUser(match.getMatchResult().getFirstPlayer(), match.getTournament());
        TournamentUser secondPlayerInTournament = TestEntityGenerator.generateTournamentUser(match.getMatchResult().getSecondPlayer(), match.getTournament());

        double firstPlayerScoreBeforeResult = match.getMatchResult().getFirstPlayer().getScore();
        double secondPlayerScoreBeforeResult = match.getMatchResult().getSecondPlayer().getScore();

        double firstPlayerScoreInTournament = firstPlayerInTournament.getScore();
        double secondPlayerScoreInTournament = secondPlayerInTournament.getScore();

        when(playerRepository.findById(match.getMatchResult().getFirstPlayer().getId()))
                .thenReturn(Optional.of(match.getMatchResult().getFirstPlayer()));
        when(playerRepository.findById(match.getMatchResult().getSecondPlayer().getId())).
                thenReturn(Optional.of(match.getMatchResult().getSecondPlayer()));
        when(tournamentUserRepository.findById(new TournamentUserKey(match.getTournament().getId(), match.getMatchResult().getFirstPlayer().getId())))
                .thenReturn(Optional.of(firstPlayerInTournament));
        when(tournamentUserRepository.findById(new TournamentUserKey(match.getTournament().getId(), match.getMatchResult().getSecondPlayer().getId())))
                .thenReturn(Optional.of(secondPlayerInTournament));

        //when
        resultService.applyResults(firstPlayerInTournament.getUser().getId(),
                secondPlayerInTournament.getUser().getId(),
                Result.FIRST,
                firstPlayerInTournament.getTournament().getId());

        //then
        assertThat(match.getMatchResult().getFirstPlayer().getScore()).isEqualTo(firstPlayerScoreBeforeResult + 1);
        assertThat(match.getMatchResult().getSecondPlayer().getScore()).isEqualTo(secondPlayerScoreBeforeResult);
        assertThat(firstPlayerInTournament.getScore()).isEqualTo(firstPlayerScoreInTournament + 1);
        assertThat(secondPlayerInTournament.getScore()).isEqualTo(secondPlayerScoreInTournament);
    }

    @Test
    public void testApplyResultsSuccessfullyResultSecond() {

        //given
        TournamentUser firstPlayerInTournament = TestEntityGenerator.generateTournamentUser(match.getMatchResult().getFirstPlayer(), match.getTournament());
        TournamentUser secondPlayerInTournament = TestEntityGenerator.generateTournamentUser(match.getMatchResult().getSecondPlayer(), match.getTournament());

        double firstPlayerScoreBeforeResult = match.getMatchResult().getFirstPlayer().getScore();
        double secondPlayerScoreBeforeResult = match.getMatchResult().getSecondPlayer().getScore();

        double firstPlayerScoreInTournament = firstPlayerInTournament.getScore();
        double secondPlayerScoreInTournament = secondPlayerInTournament.getScore();

        when(playerRepository.findById(match.getMatchResult().getFirstPlayer().getId()))
                .thenReturn(Optional.of(match.getMatchResult().getFirstPlayer()));
        when(playerRepository.findById(match.getMatchResult().getSecondPlayer().getId())).
                thenReturn(Optional.of(match.getMatchResult().getSecondPlayer()));
        when(tournamentUserRepository.findById(new TournamentUserKey(match.getTournament().getId(), match.getMatchResult().getFirstPlayer().getId())))
                .thenReturn(Optional.of(firstPlayerInTournament));
        when(tournamentUserRepository.findById(new TournamentUserKey(match.getTournament().getId(), match.getMatchResult().getSecondPlayer().getId())))
                .thenReturn(Optional.of(secondPlayerInTournament));

        //when
        resultService.applyResults(firstPlayerInTournament.getUser().getId(),
                secondPlayerInTournament.getUser().getId(),
                Result.SECOND,
                firstPlayerInTournament.getTournament().getId());

        //then
        assertThat(match.getMatchResult().getFirstPlayer().getScore()).isEqualTo(firstPlayerScoreBeforeResult);
        assertThat(match.getMatchResult().getSecondPlayer().getScore()).isEqualTo(secondPlayerScoreBeforeResult + 1);
        assertThat(firstPlayerInTournament.getScore()).isEqualTo(firstPlayerScoreInTournament);
        assertThat(secondPlayerInTournament.getScore()).isEqualTo(secondPlayerScoreInTournament + 1);
    }
}
