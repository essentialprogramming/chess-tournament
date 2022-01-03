package com.api.service;

import com.api.entities.*;
import com.api.model.GameState;
import com.api.model.Result;
import com.api.output.MatchJSON;
import com.api.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.util.TestEntityGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException;

import java.security.GeneralSecurityException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MatchServiceTest {

    @InjectMocks
    private MatchService matchService;

    @Mock
    private TournamentService tournamentService;

    @Mock
    private ResultService resultService;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private TournamentUserRepository tournamentUserRepository;

    @Mock
    private RoundRepository roundRepository;

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
    public void assignRefereeToMatchSuccessfully() {

        //given
        User referee = TestEntityGenerator.generateUser();

        when(matchRepository.findByMatchKey(match.getMatchKey())).thenReturn(Optional.of(match));
        when(userRepository.findByUserKey(referee.getUserKey())).thenReturn(Optional.of(referee));

        //when
        matchService.assignRefereeToMatch(referee.getUserKey(), match.getMatchKey());

        //then
        assertThat(match.getReferee().getUserKey()).isEqualTo(referee.getUserKey());
        assertThat(match.getReferee().getEmail()).isEqualTo(referee.getEmail());
        assertThat(match.getReferee().getFirstName()).isEqualTo(referee.getFirstName());
        assertThat(match.getReferee().getLastName()).isEqualTo(referee.getLastName());
    }

    @Test
    public void reportMatchByRefereeSuccessfully() throws JsonProcessingException {

        //given
        match.getMatchResult().setResult(null);

        when(matchRepository.findByMatchKey(match.getMatchKey())).thenReturn(Optional.of(match));
        when(roundRepository.findByRoundKey(match.getRound().getRoundKey())).thenReturn(Optional.of(match.getRound()));
        when(tournamentService.roundFinished(match.getRound())).thenReturn(true);

        //when
        MatchJSON result = matchService.reportMatchByReferee(match.getMatchKey(), Result.DRAW.getValue());

        //then
        assertThat(match.getMatchResult().getResult()).isEqualTo(Result.valueOf(result.getMatchResult()));
        assertThat(result.getState()).isEqualTo(GameState.ENDED.toString());
    }

    @Test
    public void firstResultReportSuccessfully() throws GeneralSecurityException, JsonProcessingException {

        //given
        double firstPlayerScoreBeforeResult = match.getMatchResult().getFirstPlayer().getScore();
        double secondPlayerScoreBeforeResult = match.getMatchResult().getSecondPlayer().getScore();
        TournamentUser firstPlayerInTournament = TestEntityGenerator.generateTournamentUser(match.getMatchResult().getFirstPlayer(), match.getTournament());
        TournamentUser secondPlayerInTournament = TestEntityGenerator.generateTournamentUser(match.getMatchResult().getSecondPlayer(), match.getTournament());
        firstPlayerInTournament.setScore(20);
        secondPlayerInTournament.setScore(20);
        double firstPlayerScoreInTournament = firstPlayerInTournament.getScore();
        double secondPlayerScoreInTournament = secondPlayerInTournament.getScore();
        match.getMatchResult().setResult(null);

        when(matchRepository.findByMatchKey(match.getMatchKey())).thenReturn(Optional.of(match));
        when(playerRepository.findByUserKey(match.getMatchResult().getFirstPlayer().getUserKey()))
                .thenReturn(Optional.of(match.getMatchResult().getFirstPlayer()));

        //when
        MatchJSON result = matchService.reportMatchByPlayer(match.getMatchResult().getFirstPlayer().getUserKey(), match.getMatchKey(), Result.DRAW.getValue());

        //then
        assertThat(result.getMatchResult()).isNull();
        assertThat(match.getMatchResult().getFirstPlayer().getScore()).isEqualTo(firstPlayerScoreBeforeResult);
        assertThat(match.getMatchResult().getSecondPlayer().getScore()).isEqualTo(secondPlayerScoreBeforeResult);
        assertThat(firstPlayerInTournament.getScore()).isEqualTo(firstPlayerScoreInTournament);
        assertThat(secondPlayerInTournament.getScore()).isEqualTo(secondPlayerScoreInTournament);
        assertThat(match.getMatchResult().getFirstPlayerResult()).isEqualTo(Result.DRAW);
        assertThat(match.getState()).isEqualTo(GameState.CREATED); //TODO : created or active?
    }

    @Test
    public void lastResultReportSuccessfully() throws GeneralSecurityException, JsonProcessingException {

        //given
        TournamentUser firstPlayerInTournament = TestEntityGenerator.generateTournamentUser(match.getMatchResult().getFirstPlayer(), match.getTournament());
        TournamentUser secondPlayerInTournament = TestEntityGenerator.generateTournamentUser(match.getMatchResult().getSecondPlayer(), match.getTournament());
        firstPlayerInTournament.setScore(20);
        secondPlayerInTournament.setScore(20);
        match.getMatchResult().setResult(null);
        match.getMatchResult().setFirstPlayerResult(Result.SECOND);

        when(matchRepository.findByMatchKey(match.getMatchKey())).thenReturn(Optional.of(match));
        when(playerRepository.findByUserKey(match.getMatchResult().getSecondPlayer().getUserKey()))
                .thenReturn(Optional.of(match.getMatchResult().getSecondPlayer()));

        //when
        MatchJSON result = matchService.reportMatchByPlayer(match.getMatchResult().getSecondPlayer().getUserKey(), match.getMatchKey(), Result.SECOND.getValue());

        //then
        assertThat(result.getMatchResult()).isEqualTo(Result.SECOND.toString());
        assertThat(match.getMatchResult().getSecondPlayerResult()).isEqualTo(Result.SECOND);
        assertThat(match.getState()).isEqualTo(GameState.ENDED);
    }

    @Test
    public void reportMatchByRefereeOnEndedMatch() {

        //given
        match.setState(GameState.ENDED);
        match.setReferee(TestEntityGenerator.generateUser());
        when(matchRepository.findByMatchKey(match.getMatchKey())).thenReturn(Optional.of(match));

        //when
        Throwable throwable = catchThrowable(() -> matchService.reportMatchByReferee(match.getMatchKey(), String.valueOf(Result.FIRST)));

        //then
        assertThat(throwable).isInstanceOf(HttpClientErrorException.class).hasMessage("422 This match ended, cannot report a new result!");
    }

    @Test
    public void reportMatchInvalidResult() {

        //given
        match.setReferee(TestEntityGenerator.generateUser());
        when(matchRepository.findByMatchKey(match.getMatchKey())).thenReturn(Optional.of(match));

        //when
        Throwable throwable = catchThrowable(() -> matchService.reportMatchByReferee(match.getMatchKey(), "7 - 2"));

        //then
        assertThat(throwable).isInstanceOf(HttpClientErrorException.class).hasMessage("422 Invalid result!");
    }

    @Test
    public void playerNotAllowedToReportOthersMatch() {

        //given
        Player player = TestEntityGenerator.generatePlayer(false, true, true);
        player.setId(-20);
        match.getMatchResult().getFirstPlayer().setId(1);
        match.getMatchResult().getSecondPlayer().setId(2);
        when(matchRepository.findByMatchKey(match.getMatchKey())).thenReturn(Optional.of(match));
        when(playerRepository.findByUserKey(player.getUserKey())).thenReturn(Optional.of(player));

        //when
        Throwable throwable = catchThrowable(() -> matchService.reportMatchByPlayer(player.getUserKey(), match.getMatchKey(), "1 - 0"));

        //then
        assertThat(throwable).isInstanceOf(HttpClientErrorException.class).hasMessage("422 This player is not allowed to add the result for this match!");
    }

    @Test
    public void reportMatchByPlayerOnEndedMatch() {

        //given
        match.setState(GameState.ENDED);
        when(matchRepository.findByMatchKey(match.getMatchKey())).thenReturn(Optional.of(match));

        //when
        Throwable throwable = catchThrowable(() -> matchService.reportMatchByPlayer(match.getMatchResult().getFirstPlayer().getUserKey(), match.getMatchKey(), String.valueOf(Result.FIRST)));

        //then
        assertThat(throwable).isInstanceOf(HttpClientErrorException.class).hasMessage("422 This match ended, cannot report a new result!");
    }

    @Test
    public void lastResultReportFirstPlayerWinSuccessfully() throws GeneralSecurityException, JsonProcessingException {

        //given
        TournamentUser firstPlayerInTournament = TestEntityGenerator.generateTournamentUser(match.getMatchResult().getFirstPlayer(), match.getTournament());
        TournamentUser secondPlayerInTournament = TestEntityGenerator.generateTournamentUser(match.getMatchResult().getSecondPlayer(), match.getTournament());
        match.getMatchResult().setResult(null);
        match.getMatchResult().setSecondPlayerResult(Result.FIRST);
        match.setReferee(TestEntityGenerator.generateUser());

        when(matchRepository.findByMatchKey(match.getMatchKey())).thenReturn(Optional.of(match));
        when(playerRepository.findByUserKey(match.getMatchResult().getFirstPlayer().getUserKey()))
                .thenReturn(Optional.of(match.getMatchResult().getFirstPlayer()));

        //when
        MatchJSON result = matchService.reportMatchByPlayer(match.getMatchResult().getFirstPlayer().getUserKey(), match.getMatchKey(), Result.FIRST.getValue());

        //then
        assertThat(result.getMatchResult()).isEqualTo(Result.FIRST.toString());
        assertThat(match.getMatchResult().getSecondPlayerResult()).isEqualTo(Result.FIRST);
        assertThat(match.getState()).isEqualTo(GameState.ENDED);
    }
}

