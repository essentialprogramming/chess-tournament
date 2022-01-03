package com.api.service;

import com.api.entities.*;
import com.api.mapper.ParticipantStatusMapper;
import com.api.model.*;
import com.api.output.ParticipantStatusJSON;
import com.api.output.RoundJSON;
import com.api.entities.Schedule;
import com.api.entities.Tournament;
import com.api.entities.User;
import com.api.model.GameState;
import com.api.model.ScheduleInput;
import com.api.model.TournamentInput;
import com.api.model.TournamentStatus;
import com.api.output.TournamentJSON;
import com.api.repository.*;
import com.email.service.EmailManager;
import com.util.TestEntityGenerator;
import com.util.TestMockUtil;
import com.util.web.JsonResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TournamentServiceTest {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int MAX_PARTICIPANTS = 100;
    private static final int MIN_PARTICIPANTS = 3;
    private static final int TOURNAMENTS_AMOUNT = 15;

    @InjectMocks
    private TournamentService tournamentService;

    @Mock
    private EmailManager emailManager;

    @Mock
    private ResultService resultService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private TournamentUserRepository tournamentUserRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchPlayerRepository matchPlayerRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private RoundRepository roundRepository;

    @Mock
    private MatchResultRepository matchResultRepository;

    public MockedStatic<WebSocketManager> wsMockStatic;

    @AfterEach
    public void afterEach() {
        if (wsMockStatic != null) {
            wsMockStatic.close();
        }
    }

    @Test
    public void addTournamentSuccessfully() {

        //given

        TournamentInput tournamentInput = TestEntityGenerator.generateTournamentInput(true);

        ScheduleInput scheduleInput = TestEntityGenerator.generateScheduleInput();
        tournamentInput.setSchedule(scheduleInput);

        when(tournamentRepository.save(any())).thenReturn(any(Tournament.class));

        //when

        TournamentJSON result = tournamentService.addTournament(tournamentInput);

        //then

        assertThat(result.getName()).isEqualTo(tournamentInput.getName());
        assertThat(result.isRegistrationOpen()).isEqualTo(tournamentInput.isRegistrationOpen());
        assertThat(result.getMaxParticipantsNo()).isEqualTo(tournamentInput.getMaxParticipantsNo());
        assertThat(result.getSchedule().getStartDate()).isEqualTo(tournamentInput.getSchedule().getStartDate());
        assertThat(result.getSchedule().getEndDate()).isEqualTo(tournamentInput.getSchedule().getEndDate());
        assertThat(result.getSchedule().getLocation()).isEqualTo(tournamentInput.getSchedule().getLocation());
        assertThat(result.getTournamentKey()).isNotNull();
    }

    @Test
    public void registerPlayerToTournamentSuccessfully() {

        //given

        Player player = TestEntityGenerator.generatePlayer(false, true, false);
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.CREATED, true, 20);
        Schedule schedule = TestEntityGenerator.generateSchedule();
        UserSettings userSettings = TestEntityGenerator.generateUserSettings(true, Type.TOURNAMENT_INVITATION);

        userSettings.setTournament(tournament);
        userSettings.setPlayer(player);

        player.getUserSettings().add(userSettings);

        tournament.setSchedule(schedule);

        when(playerRepository.findByUserKey(player.getUserKey())).thenReturn(Optional.of(player));
        when(tournamentRepository.findByTournamentKey(tournament.getTournamentKey())).thenReturn(Optional.of(tournament));
        doNothing().when(emailManager).send(any(), any(), any(), any(), any());
        when(userSettingsRepository.findByPlayerAndTournament(player, tournament)).thenReturn(Optional.of(userSettings));

        //when

        JsonResponse result = tournamentService.registerPlayer(player.getUserKey(), tournament.getTournamentKey());

        //then

        assertThat(result.getResponse().get("status")).isEqualTo("ok");
        assertThat(result.getResponse().get("userKey")).isEqualTo(player.getUserKey());
        assertThat(tournament.getPlayers().contains(player)).isTrue();
        assertThat(player.getScore()).isZero();
        assertThat(player.getUserSettings().contains(userSettings)).isFalse();

        verify(emailManager).send(any(), any(), any(), any(), any());
        verify(playerRepository).findByUserKey(player.getUserKey());
        verify(tournamentRepository).findByTournamentKey(tournament.getTournamentKey());
        verify(userSettingsRepository).findByPlayerAndTournament(player, tournament);
    }

    @Test
    public void registerPlayerToTournamentFailRegistrationClosed() {

        //given

        Player player = TestEntityGenerator.generatePlayer(false, true, false);
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.CREATED, false, 20);
        Schedule schedule = TestEntityGenerator.generateSchedule();
        UserSettings userSettings = TestEntityGenerator.generateUserSettings(true, Type.TOURNAMENT_INVITATION);

        userSettings.setTournament(tournament);
        userSettings.setPlayer(player);

        player.getUserSettings().add(userSettings);

        tournament.setSchedule(schedule);

        when(playerRepository.findByUserKey(player.getUserKey())).thenReturn(Optional.of(player));
        when(tournamentRepository.findByTournamentKey(tournament.getTournamentKey())).thenReturn(Optional.of(tournament));
        doNothing().when(emailManager).send(any(), any(), any(), any(), any());

        //when

        Throwable throwable = catchThrowable(() -> tournamentService.registerPlayer(player.getUserKey(), tournament.getTournamentKey()));

        //then

        assertThat(throwable).isInstanceOf(HttpClientErrorException.class).hasMessage("422 Tournament participation is disabled!");
        verify(playerRepository).findByUserKey(player.getUserKey());
        verify(tournamentRepository).findByTournamentKey(tournament.getTournamentKey());
    }

    @Test
    public void registerPlayerToTournamentFailMaxParticipants() {

        //given

        Player player = TestEntityGenerator.generatePlayer(false, true, false);
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.CREATED, true, 0);
        Schedule schedule = TestEntityGenerator.generateSchedule();
        UserSettings userSettings = TestEntityGenerator.generateUserSettings(true, Type.TOURNAMENT_INVITATION);

        userSettings.setTournament(tournament);
        userSettings.setPlayer(player);

        player.getUserSettings().add(userSettings);

        tournament.setSchedule(schedule);

        when(playerRepository.findByUserKey(player.getUserKey())).thenReturn(Optional.of(player));
        when(tournamentRepository.findByTournamentKey(tournament.getTournamentKey())).thenReturn(Optional.of(tournament));
        doNothing().when(emailManager).send(any(), any(), any(), any(), any());

        //when

        Throwable throwable = catchThrowable(() -> tournamentService.registerPlayer(player.getUserKey(), tournament.getTournamentKey()));

        //then

        assertThat(throwable).isInstanceOf(HttpClientErrorException.class).hasMessage("422 Tournament participation is disabled!");
        verify(playerRepository).findByUserKey(player.getUserKey());
        verify(tournamentRepository).findByTournamentKey(tournament.getTournamentKey());
    }

    @Test
    public void registerPlayerToTournamentFailAlreadyRegistered() {

        //given

        Player player = TestEntityGenerator.generatePlayer(false, true, false);
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.CREATED, true, 20);
        Schedule schedule = TestEntityGenerator.generateSchedule();
        UserSettings userSettings = TestEntityGenerator.generateUserSettings(true, Type.TOURNAMENT_INVITATION);

        tournament.getPlayers().add(player);

        userSettings.setTournament(tournament);
        userSettings.setPlayer(player);

        player.getUserSettings().add(userSettings);

        tournament.setSchedule(schedule);

        when(playerRepository.findByUserKey(player.getUserKey())).thenReturn(Optional.of(player));
        when(tournamentRepository.findByTournamentKey(tournament.getTournamentKey())).thenReturn(Optional.of(tournament));

        //when

        Throwable throwable = catchThrowable(() -> tournamentService.registerPlayer(player.getUserKey(), tournament.getTournamentKey()));

        //then

        assertThat(throwable).isInstanceOf(HttpClientErrorException.class).hasMessage("422 Player is already participating in the tournament!");
        verify(playerRepository).findByUserKey(player.getUserKey());
        verify(tournamentRepository).findByTournamentKey(tournament.getTournamentKey());
    }

    @Test
    public void registerPlayerToTournamentFailPlayerNotFound() {

        //given

        Player player = TestEntityGenerator.generatePlayer(false, true, false);
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.CREATED, true, 20);
        Schedule schedule = TestEntityGenerator.generateSchedule();
        UserSettings userSettings = TestEntityGenerator.generateUserSettings(true, Type.TOURNAMENT_INVITATION);

        tournament.getPlayers().add(player);

        userSettings.setTournament(tournament);
        userSettings.setPlayer(player);

        player.getUserSettings().add(userSettings);

        tournament.setSchedule(schedule);

        when(playerRepository.findByUserKey(player.getUserKey())).thenReturn(Optional.empty());

        //when

        Throwable throwable = catchThrowable(() -> tournamentService.registerPlayer(player.getUserKey(), tournament.getTournamentKey()));

        //then

        assertThat(throwable).isInstanceOf(HttpClientErrorException.class).hasMessage("404 Player not found!");
        verify(playerRepository).findByUserKey(player.getUserKey());
    }

    @Test
    public void registerPlayerToTournamentFailTournamentNotFound() {

        //given

        Player player = TestEntityGenerator.generatePlayer(false, true, false);
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.CREATED, true, 20);
        Schedule schedule = TestEntityGenerator.generateSchedule();
        UserSettings userSettings = TestEntityGenerator.generateUserSettings(true, Type.TOURNAMENT_INVITATION);

        tournament.getPlayers().add(player);

        userSettings.setTournament(tournament);
        userSettings.setPlayer(player);

        player.getUserSettings().add(userSettings);

        tournament.setSchedule(schedule);

        when(playerRepository.findByUserKey(player.getUserKey())).thenReturn(Optional.of(player));
        when(tournamentRepository.findByTournamentKey(tournament.getTournamentKey())).thenReturn(Optional.empty());

        //when

        Throwable throwable = catchThrowable(() -> tournamentService.registerPlayer(player.getUserKey(), tournament.getTournamentKey()));

        //then

        assertThat(throwable).isInstanceOf(HttpClientErrorException.class).hasMessage("404 Tournament not found!");
        verify(playerRepository).findByUserKey(player.getUserKey());
        verify(tournamentRepository).findByTournamentKey(tournament.getTournamentKey());
    }

    @Test
    public void assignRefereeToTournamentSuccessfully(){

        //given
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.CREATED, true,
                secureRandom.nextInt(MAX_PARTICIPANTS - MIN_PARTICIPANTS) + MIN_PARTICIPANTS);
        User referee = TestEntityGenerator.generateUser();
        Schedule schedule = TestEntityGenerator.generateSchedule();

        Set<User> referees = new HashSet<>();

        tournament.setSchedule(schedule);
        tournament.setReferees(referees);

        when(tournamentRepository.findByTournamentKey(any())).thenReturn(Optional.of(tournament));
        when(userRepository.findByUserKey(any())).thenReturn(Optional.of(referee));

        //when
        JsonResponse result = tournamentService.assignRefereeToTournament(referee.getUserKey(), tournament.getTournamentKey());

        //then
        assertThat(result.getResponse().get("status")).isEqualTo("ok");
        assertThat(result.getResponse().get("message")).isEqualTo("Referee added in tournament!");
        assertThat(tournament.getReferees().contains(referee)).isEqualTo(true);
    }

    @Test
    public void listAllTournamentParticipants() {

        //given
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.CREATED, true, 20);
        Player player = TestEntityGenerator.generatePlayer(false, true, false);

        List<UserSettings> playersInvited = new ArrayList<>();
        List<TournamentUser> playersAccepted = new ArrayList<>();

        for (int noPlayersInvited = 3; noPlayersInvited > 0; noPlayersInvited--) {
            UserSettings userSettings = TestEntityGenerator.generateUserSettings(true, Type.TOURNAMENT_INVITATION);
            userSettings.setTournament(tournament);
            userSettings.setPlayer(TestEntityGenerator.generatePlayer(false, true, false));

            playersInvited.add(userSettings);
        }

        List<ParticipantStatusJSON> participantStatusJSONListAccepted = playersInvited.stream()
                .map(playerInvited -> ParticipantStatusMapper.createJSON(playerInvited.getPlayer(),
                        InvitationStatus.PENDING))
                .collect(Collectors.toList());

        for (int noPlayersAccepted = 2; noPlayersAccepted > 0; noPlayersAccepted--) {
            TournamentUser tournamentUser = new TournamentUser(tournament, TestEntityGenerator.generateUser());

            playersAccepted.add(tournamentUser);
        }

        List<ParticipantStatusJSON> participantStatusJSONListInvited = playersAccepted.stream()
                .map(playerAccepted -> ParticipantStatusMapper.createJSON(playerAccepted.getUser(),
                        InvitationStatus.ACCEPTED))
                .collect(Collectors.toList());

        when(tournamentRepository.findByTournamentKey(tournament.getTournamentKey())).thenReturn(Optional.of(tournament));
        when(userSettingsRepository.findByTournament(tournament)).thenReturn(playersInvited);
        when(tournamentUserRepository.findUserByTournament(tournament)).thenReturn(playersAccepted);

        List<ParticipantStatusJSON> allParticipants = Stream.concat(participantStatusJSONListInvited.stream(), participantStatusJSONListAccepted.stream()).collect(Collectors.toList());

        //when
        List<ParticipantStatusJSON> result = tournamentService.listAllParticipants(tournament.getTournamentKey());

        //then
        for (int noParticipants = 0; noParticipants < 5; noParticipants++) {
            assertThat(result.get(noParticipants).getUserJSON().getEmail()).isEqualTo(allParticipants.get(noParticipants).getUserJSON().getEmail());
            assertThat(result.get(noParticipants).getUserJSON().getUserKey()).isEqualTo(allParticipants.get(noParticipants).getUserJSON().getUserKey());
            assertThat(result.get(noParticipants).getUserJSON().getFirstName()).isEqualTo(allParticipants.get(noParticipants).getUserJSON().getFirstName());
            assertThat(result.get(noParticipants).getUserJSON().getLastName()).isEqualTo(allParticipants.get(noParticipants).getUserJSON().getLastName());
            assertThat(result.get(noParticipants).getUserJSON().getPhone()).isEqualTo(allParticipants.get(noParticipants).getUserJSON().getPhone());
            assertThat(result.get(noParticipants).getStatus()).isEqualTo(allParticipants.get(noParticipants).getStatus());
        }
    }

    @Test
    public void startTournamentSuccessfully() {

        //given
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.CREATED, true,
                secureRandom.nextInt(MAX_PARTICIPANTS - MIN_PARTICIPANTS) + MIN_PARTICIPANTS);
        List<Player> playerList = TestEntityGenerator.generatePlayerList(secureRandom.nextInt(MAX_PARTICIPANTS - MIN_PARTICIPANTS) + MIN_PARTICIPANTS);
        tournament.setPlayers(playerList);

        int currentPlayerSize = playerList.size();

        when(tournamentRepository.findByTournamentKey(tournament.getTournamentKey())).thenReturn(Optional.of(tournament));

        when(roundRepository.save(any())).thenReturn(null);

        when(matchRepository.save(any())).thenReturn(null);

        when(matchPlayerRepository.save(any())).thenReturn(null);

        when(matchResultRepository.save(any())).thenReturn(null);

        wsMockStatic = TestMockUtil.mockStaticWSM();


        //when
        Throwable throwable = catchThrowable(() -> tournamentService.startTournament(tournament.getTournamentKey()));


        //then
        assertThat(throwable).isNull();
        assertThat(tournament.getCurrentRound()).isNotNull();
        assertThat(tournament.getRounds()).isNotNull();
        assertThat(tournament.getRounds().size()).isEqualTo(tournament.getPlayers().size() - 1);
        assertThat(tournament.getCurrentRound().getState()).isEqualTo(GameState.ACTIVE);
        assertThat(tournament.isRegistrationOpen()).isEqualTo(false);
        assertThat(tournament.getState()).isEqualTo(GameState.ACTIVE);

        if (currentPlayerSize % 2 != 0) {
            assertThat(tournament.getCurrentRound().getMatches().get(0).getState()).isEqualTo(GameState.ENDED);
        } else {
            assertThat(tournament.getCurrentRound().getMatches().get(0).getState()).isEqualTo(GameState.ACTIVE);
        }
        assertThat(tournament.getCurrentRound().getMatches().get(1).getState()).isEqualTo(GameState.ACTIVE);
    }

    @Test
    public void startTournamentFailNoParticipants() {

        //given
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.CREATED, true,
                secureRandom.nextInt(MAX_PARTICIPANTS - MIN_PARTICIPANTS) + MIN_PARTICIPANTS);
        List<Player> playerList = new ArrayList<>();
        tournament.setPlayers(playerList);

        when(tournamentRepository.findByTournamentKey(tournament.getTournamentKey())).thenReturn(Optional.of(tournament));


        //when
        Throwable throwable = catchThrowable(() -> tournamentService.startTournament(tournament.getTournamentKey()));


        //then
        assertThat(throwable).isInstanceOf(HttpClientErrorException.class).hasMessage("422 This tournament currently has no participants!");
        assertThat(tournament.getCurrentRound()).isNull();
        assertThat(tournament.getState()).isNotEqualByComparingTo(GameState.ACTIVE);
    }

    @Test
    public void startTournamentFailTournamentNotFound() {

        //given
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.CREATED, true,
                secureRandom.nextInt(MAX_PARTICIPANTS - MIN_PARTICIPANTS) + MIN_PARTICIPANTS);
        when(tournamentRepository.findByTournamentKey(tournament.getTournamentKey())).thenReturn(Optional.empty());


        //when
        Throwable throwable = catchThrowable(() -> tournamentService.startTournament(tournament.getTournamentKey()));


        //then
        assertThat(throwable).isInstanceOf(HttpClientErrorException.class).hasMessage("404 Tournament not found");
    }

    @Test
    public void startTournamentFailAlreadyStarted() {

        //given
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.ACTIVE, true,
                secureRandom.nextInt(MAX_PARTICIPANTS - MIN_PARTICIPANTS) + MIN_PARTICIPANTS);
        when(tournamentRepository.findByTournamentKey(tournament.getTournamentKey())).thenReturn(Optional.of(tournament));


        //when
        when(tournamentRepository.findByTournamentKey(tournament.getTournamentKey())).thenReturn(Optional.of(tournament));
        Throwable throwable = catchThrowable(() -> tournamentService.startTournament(tournament.getTournamentKey()));


        //then
        assertThat(throwable).isInstanceOf(HttpClientErrorException.class).hasMessage("422 Tournament has already started!");
    }

    @Test
    public void startTournamentFailAlreadyEnded() {

        //given
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.ENDED, false,
                secureRandom.nextInt(MAX_PARTICIPANTS - MIN_PARTICIPANTS) + MIN_PARTICIPANTS);
        when(tournamentRepository.findByTournamentKey(tournament.getTournamentKey())).thenReturn(Optional.of(tournament));


        //when
        Throwable throwable = catchThrowable(() -> tournamentService.startTournament(tournament.getTournamentKey()));


        //then
        assertThat(throwable).isInstanceOf(HttpClientErrorException.class).hasMessage("422 Tournament has already ended!");
    }

    @Test

    public void setRoundSuccessfully() {

        //given
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.CREATED, true, 20);
        Round round = TestEntityGenerator.generateRound(GameState.CREATED);
        Round round2 = TestEntityGenerator.generateRound(GameState.CREATED);
        Player player = TestEntityGenerator.generatePlayer(false, true, true);
        Player player1 = TestEntityGenerator.generatePlayer(false, true, true);

        tournament.setCurrentRound(round2);
        round.setTournament(tournament);
        round.setNumber(1);

        Match match = TestEntityGenerator.generateMatch(GameState.CREATED);

        round.setMatches(Collections.singletonList(match));
        match.setRound(round);
        match.setTournament(tournament);

        MatchResult matchResult = TestEntityGenerator.generateMatchResult(Result.SECOND);

        matchResult.setFirstPlayer(player);
        matchResult.setSecondPlayer(player1);
        match.setMatchResult(matchResult);

        when(matchRepository.save(any())).thenReturn(match);
        when(tournamentRepository.findByTournamentKey(tournament.getTournamentKey())).thenReturn(Optional.of(tournament));
        when(roundRepository.findByRoundKey(round.getRoundKey())).thenReturn(Optional.of(round));

        //when
        RoundJSON result = tournamentService.setRound(tournament.getTournamentKey(), round.getRoundKey());

        //then
        assertThat(round.getState()).isEqualTo(GameState.ACTIVE);
        assertThat(result.getRoundKey()).isEqualTo(round.getRoundKey());
        assertThat(result.getNumber()).isEqualTo(round.getNumber());
        assertThat(result.getTournamentKey()).isEqualTo(round.getTournament().getTournamentKey());
        assertThat(result.getMatches().size()).isEqualTo(round.getMatches().size());
    }

    @Test
    public void invitePlayerToTournamentSuccessfully(){

        //given
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.ACTIVE, true, 30);
        Player player = TestEntityGenerator.generatePlayer(false,true,false);
        Schedule schedule = TestEntityGenerator.generateSchedule();
        UserSettings userSettings = TestEntityGenerator.generateUserSettings(true, Type.TOURNAMENT_INVITATION);

        ArrayList<Player> players = new ArrayList<>();
        players.add(player);

        userSettings.setTournament(tournament);
        userSettings.setPlayer(player);

        player.getUserSettings().add(userSettings);

        tournament.setSchedule(schedule);
        tournament.setPlayers(players);

        when(playerRepository.findByUserKey(any())).thenReturn(Optional.of(player));
        when(tournamentRepository.findByTournamentKey(any())).thenReturn(Optional.of(tournament));

        //when
        JsonResponse result = tournamentService.invitePlayer(tournament.getTournamentKey(), player.getUserKey());

        //then
        assertThat(result.getResponse().get("status")).isEqualTo("ok");
        assertThat(result.getResponse().get("message")).isEqualTo("Player invited to the tournament!");
        assertThat(player.getUserSettings().contains(userSettings)).isEqualTo(true);

        verify(emailManager).send(any(), any(), any(), any(), any());

    }

    @Test
    public void setMaxParticipantsSuccessfully(){

        //given
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.CREATED, true, 30);
        int maxParticipants =  secureRandom.nextInt();

        when(tournamentRepository.findByTournamentKey(any())).thenReturn(Optional.of(tournament));

        //when
        JsonResponse result = tournamentService.setMaxParticipants(tournament.getTournamentKey(), maxParticipants);

        //then
        assertThat(tournament.getMaxParticipants()).isEqualTo(maxParticipants);
        assertThat(result.getResponse().get("status")).isEqualTo("ok");
        assertThat(result.getResponse().get("message")).isEqualTo((String.format("Maximum number of participants set to %d for tournament %s",maxParticipants, tournament.getName())));
    }

    //@Test
    //TODO Fix broken unit test
    public void switchToNextRoundSuccessfully() {

        //given
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.CREATED, true, 20);
        Round firstRound = TestEntityGenerator.generateRound(GameState.CREATED);
        Round secondRound = TestEntityGenerator.generateRound(GameState.CREATED);
        Round thirdRound = TestEntityGenerator.generateRound(GameState.CREATED);

        List<Match> firstRoundMatches = new ArrayList<>();
        List<Match> secondRoundMatches = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            Match firstRoundMatch = TestEntityGenerator.generateMatch(GameState.ENDED);
            firstRoundMatch.setTournament(tournament);
            firstRoundMatch.setRound(firstRound);

            Match secondRoundMatch = TestEntityGenerator.generateMatch(GameState.CREATED);
            secondRoundMatch.setTournament(tournament);
            secondRoundMatch.setRound(secondRound);

            firstRoundMatches.add(firstRoundMatch);
            secondRoundMatches.add(secondRoundMatch);
        }

        firstRound.setTournament(tournament);
        firstRound.setState(GameState.ENDED);
        firstRound.setMatches(firstRoundMatches);

        secondRound.setTournament(tournament);
        secondRound.setState(GameState.ACTIVE);
        secondRound.setNumber(firstRound.getNumber() + 1);
        secondRound.setMatches(secondRoundMatches);

        thirdRound.setTournament(tournament);
        thirdRound.setNumber(secondRound.getNumber() + 1);

        tournament.setCurrentRound(firstRound);
        tournament.setState(GameState.ACTIVE);

        Map<Integer, Round> rounds = new HashMap<>();
        rounds.put(firstRound.getNumber(), firstRound);
        rounds.put(secondRound.getNumber(), secondRound);
        rounds.put(thirdRound.getNumber(), thirdRound);

        tournament.setRounds(rounds);

        wsMockStatic = TestMockUtil.mockStaticWSM();

        when(tournamentRepository.findByTournamentKey(tournament.getTournamentKey())).thenReturn(Optional.of(tournament));

        //when
        RoundJSON result = tournamentService.switchToNextRound(tournament.getTournamentKey());

        //then
        assertThat(result.getRoundKey()).isEqualTo(secondRound.getRoundKey());
        assertThat(result.getTournamentKey()).isEqualTo(tournament.getTournamentKey());
        assertThat(result.getNumber()).isEqualTo(secondRound.getNumber());

        for (int i = 0; i < secondRound.getMatches().size(); i++) {
            assertThat(result.getMatches().get(i).getMatchKey())
                    .isEqualTo(secondRound.getMatches().get(i).getMatchKey());
            assertThat(result.getMatches().get(i).getTournamentKey())
                    .isEqualTo(secondRound.getMatches().get(i).getTournament().getTournamentKey());
            assertThat(result.getMatches().get(i).getStartDate())
                    .isEqualTo(secondRound.getMatches().get(i).getStartDate().toString());
            assertThat(result.getMatches().get(i).getRoundId())
                    .isEqualTo(secondRound.getMatches().get(i).getRound().getId());
            assertThat(result.getMatches().get(i).getState())
                    .isEqualTo(secondRound.getMatches().get(i).getState().toString());
        }

        verify(tournamentRepository).findByTournamentKey(tournament.getTournamentKey());
    }

    //@Test
    //TODO Fix broken unit test
    public void switchToNextRoundSuccessfullyFinalRound() {

        //given
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.CREATED, true, 20);
        Round firstRound = TestEntityGenerator.generateRound(GameState.CREATED);

        List<Match> firstRoundMatches = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            Match firstRoundMatch = TestEntityGenerator.generateMatch(GameState.ENDED);
            firstRoundMatch.setTournament(tournament);
            firstRoundMatch.setRound(firstRound);

            firstRoundMatches.add(firstRoundMatch);
        }

        firstRound.setTournament(tournament);
        firstRound.setState(GameState.ENDED);
        firstRound.setMatches(firstRoundMatches);
        firstRound.setNumber(1);

        tournament.setCurrentRound(firstRound);
        tournament.setState(GameState.ACTIVE);

        Map<Integer, Round> rounds = new HashMap<>();
        rounds.put(firstRound.getNumber(), firstRound);

        tournament.setRounds(rounds);

        wsMockStatic = TestMockUtil.mockStaticWSM();

        when(tournamentRepository.findByTournamentKey(tournament.getTournamentKey())).thenReturn(Optional.of(tournament));

        //when
        RoundJSON result = tournamentService.switchToNextRound(tournament.getTournamentKey());

        //then
        assertThat(result.getRoundKey()).isEqualTo(firstRound.getRoundKey());
        assertThat(result.getTournamentKey()).isEqualTo(tournament.getTournamentKey());
        assertThat(result.getNumber()).isEqualTo(firstRound.getNumber());
        assertThat(tournament.getState()).isEqualTo(GameState.ENDED);
        assertThat(firstRound.getState()).isEqualTo(GameState.ENDED);

        for (int i = 0; i < firstRound.getMatches().size(); i++) {
            assertThat(result.getMatches().get(i).getMatchKey())
                    .isEqualTo(firstRound.getMatches().get(i).getMatchKey());
            assertThat(result.getMatches().get(i).getTournamentKey())
                    .isEqualTo(firstRound.getMatches().get(i).getTournament().getTournamentKey());
            assertThat(result.getMatches().get(i).getStartDate())
                    .isEqualTo(firstRound.getMatches().get(i).getStartDate().toString());
            assertThat(result.getMatches().get(i).getRoundId())
                    .isEqualTo(firstRound.getMatches().get(i).getRound().getId());
            assertThat(result.getMatches().get(i).getState())
                    .isEqualTo("ENDED");
        }

        verify(tournamentRepository).findByTournamentKey(tournament.getTournamentKey());
    }

    @Test
    public void switchToNextRoundFailTournamentNotFound() {

        //given
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.CREATED, true, 20);

        when(tournamentRepository.findByTournamentKey(tournament.getTournamentKey())).thenReturn(Optional.empty());

        //when
        Throwable throwable = catchThrowable(() -> tournamentService.switchToNextRound(tournament.getTournamentKey()));

        //then
        assertThat(throwable).isInstanceOf(HttpClientErrorException.class).hasMessage("404 Tournament not found");
    }

    @Test
    public void switchToNextRoundFailTournamentNotActive() {

        //given
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.CREATED, true, 20);
        Round firstRound = TestEntityGenerator.generateRound(GameState.CREATED);
        Round secondRound = TestEntityGenerator.generateRound(GameState.CREATED);
        Round thirdRound = TestEntityGenerator.generateRound(GameState.CREATED);

        List<Match> firstRoundMatches = new ArrayList<>();
        List<Match> secondRoundMatches = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            Match firstRoundMatch = TestEntityGenerator.generateMatch(GameState.ENDED);
            firstRoundMatch.setTournament(tournament);
            firstRoundMatch.setRound(firstRound);

            Match secondRoundMatch = TestEntityGenerator.generateMatch(GameState.CREATED);
            secondRoundMatch.setTournament(tournament);
            secondRoundMatch.setRound(secondRound);

            firstRoundMatches.add(firstRoundMatch);
            secondRoundMatches.add(secondRoundMatch);
        }

        firstRound.setTournament(tournament);
        firstRound.setState(GameState.ENDED);
        firstRound.setMatches(firstRoundMatches);

        secondRound.setTournament(tournament);
        secondRound.setState(GameState.ACTIVE);
        secondRound.setMatches(secondRoundMatches);
        secondRound.setNumber(firstRound.getNumber() + 1);

        thirdRound.setTournament(tournament);
        thirdRound.setNumber(secondRound.getNumber() + 1);

        tournament.setCurrentRound(firstRound);

        Map<Integer, Round> rounds = new HashMap<>();
        rounds.put(firstRound.getNumber(), firstRound);
        rounds.put(secondRound.getNumber(), secondRound);
        rounds.put(thirdRound.getNumber(), thirdRound);

        tournament.setRounds(rounds);

        wsMockStatic = TestMockUtil.mockStaticWSM();

        when(tournamentRepository.findByTournamentKey(tournament.getTournamentKey())).thenReturn(Optional.of(tournament));

        //when
        Throwable throwable = catchThrowable(() -> tournamentService.switchToNextRound(tournament.getTournamentKey()));

        //then
        assertThat(throwable).isInstanceOf(HttpClientErrorException.class).hasMessage("422 Tournament did not start yet!");
    }

    @Test
    public void switchToNextRoundFailRoundNotFinished() {

        //given
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.CREATED, true, 20);
        Round firstRound = TestEntityGenerator.generateRound(GameState.CREATED);
        Round secondRound = TestEntityGenerator.generateRound(GameState.CREATED);
        Round thirdRound = TestEntityGenerator.generateRound(GameState.CREATED);

        List<Match> firstRoundMatches = new ArrayList<>();
        List<Match> secondRoundMatches = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            Match firstRoundMatch = TestEntityGenerator.generateMatch(GameState.ACTIVE);
            firstRoundMatch.setTournament(tournament);
            firstRoundMatch.setRound(firstRound);

            Match secondRoundMatch = TestEntityGenerator.generateMatch(GameState.CREATED);
            secondRoundMatch.setTournament(tournament);
            secondRoundMatch.setRound(secondRound);

            firstRoundMatches.add(firstRoundMatch);
            secondRoundMatches.add(secondRoundMatch);
        }

        firstRound.setTournament(tournament);
        firstRound.setState(GameState.ENDED);
        firstRound.setMatches(firstRoundMatches);

        secondRound.setTournament(tournament);
        secondRound.setState(GameState.ACTIVE);
        secondRound.setMatches(secondRoundMatches);
        secondRound.setNumber(firstRound.getNumber() + 1);

        thirdRound.setTournament(tournament);
        thirdRound.setNumber(secondRound.getNumber() + 1);

        tournament.setCurrentRound(firstRound);
        tournament.setState(GameState.ACTIVE);

        Map<Integer, Round> rounds = new HashMap<>();
        rounds.put(firstRound.getNumber(), firstRound);
        rounds.put(secondRound.getNumber(), secondRound);
        rounds.put(thirdRound.getNumber(), thirdRound);

        tournament.setRounds(rounds);

        wsMockStatic = TestMockUtil.mockStaticWSM();

        when(tournamentRepository.findByTournamentKey(tournament.getTournamentKey())).thenReturn(Optional.of(tournament));

        //when
        Throwable throwable = catchThrowable(() -> tournamentService.switchToNextRound(tournament.getTournamentKey()));

        //then
        assertThat(throwable).isInstanceOf(HttpClientErrorException.class).hasMessage("422 Round is still ongoing!");
    }

    @Test
    public void endTournamentSuccessfully() {

        //given
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.ACTIVE, true, secureRandom.nextInt((MAX_PARTICIPANTS - MIN_PARTICIPANTS) + MIN_PARTICIPANTS));
        List<Player> playerList = TestEntityGenerator.generatePlayerList(secureRandom.nextInt((MAX_PARTICIPANTS - MIN_PARTICIPANTS) + MIN_PARTICIPANTS));
        List<TournamentUser> tournamentUserList = new ArrayList<>();

        for (Player player : playerList) {
            TournamentUser tournamentUser = TestEntityGenerator.generateTournamentUser(player, tournament);
            tournamentUser.setScore(player.getScore());
            tournamentUserList.add(tournamentUser);
        }

        when(tournamentRepository.findByTournamentKey(tournament.getTournamentKey())).thenReturn(Optional.of(tournament));
        when(tournamentUserRepository.findTopByTournamentOrderByScoreDesc(any())).thenReturn(Optional.of(tournamentUserList.get(1)));
        wsMockStatic = TestMockUtil.mockStaticWSM();


        //when
        JsonResponse result = tournamentService.endTournament(tournament.getTournamentKey());


        //then
        assertThat(tournament.getState()).isEqualTo(GameState.ENDED);
        assertThat(tournament.isRegistrationOpen()).isEqualTo(false);
        assertThat(result.getResponse().get("status")).isEqualTo("ok");
        assertThat(result.getResponse().get("message")).isEqualTo("Tournament ended.");
    }

    @Test
    public void endTournamentFailTournamentNotFound() {

        //given
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.CREATED, true, secureRandom.nextInt((MAX_PARTICIPANTS - MIN_PARTICIPANTS) + MIN_PARTICIPANTS));
        when(tournamentRepository.findByTournamentKey(tournament.getTournamentKey())).thenReturn(Optional.empty());


        //when
        Throwable throwable = catchThrowable(() -> tournamentService.endTournament(tournament.getTournamentKey()));


        //then
        assertThat(throwable).isInstanceOf(HttpClientErrorException.class).hasMessage("404 Tournament not found");
    }

    @Test
    public void endTournamentFailAlreadyEnded() {

        //given
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.ENDED, true, secureRandom.nextInt((MAX_PARTICIPANTS - MIN_PARTICIPANTS) + MIN_PARTICIPANTS));


        //when
        when(tournamentRepository.findByTournamentKey(tournament.getTournamentKey())).thenReturn(Optional.of(tournament));
        Throwable throwable = catchThrowable(() -> tournamentService.endTournament(tournament.getTournamentKey()));


        //then
        assertThat(throwable).isInstanceOf(HttpClientErrorException.class).hasMessage("422 Tournament has already ended!");
    }

    @Test
    public void endTournamentFailNotStartedYet() {

        //given
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.CREATED, true, secureRandom.nextInt((MAX_PARTICIPANTS - MIN_PARTICIPANTS) + MIN_PARTICIPANTS));


        //when
        when(tournamentRepository.findByTournamentKey(tournament.getTournamentKey())).thenReturn(Optional.of(tournament));
        Throwable throwable = catchThrowable(() -> tournamentService.endTournament(tournament.getTournamentKey()));


        //then
        assertThat(throwable).isInstanceOf(HttpClientErrorException.class).hasMessage("422 Tournament did not start yet!");
    }



    @Test
    public void setRegistrationStatusToTrueSuccess() {

        //given
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.CREATED, false,
                secureRandom.nextInt(MAX_PARTICIPANTS - MIN_PARTICIPANTS) + MIN_PARTICIPANTS);
        when(tournamentRepository.findByTournamentKey(tournament.getTournamentKey())).thenReturn(Optional.of(tournament));



        //when
        Throwable throwable = catchThrowable(() -> tournamentService.setRegistrationStatus(tournament.getTournamentKey(), true));
        JsonResponse result = tournamentService.setRegistrationStatus(tournament.getTournamentKey(), true);


        //then
        assertThat(throwable).isNull();
        assertThat(tournament.isRegistrationOpen()).isEqualTo(true);
        assertThat(result.getResponse().get("status")).isEqualTo("ok");
        assertThat(result.getResponse().get("message")).isEqualTo(String.format("Registration status set to %b for tournament %s",
                tournament.isRegistrationOpen(), tournament.getName()));

    }

    @Test
    public void setRegistrationStatusToFalseSuccess() {

        //given
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.CREATED, true,
                secureRandom.nextInt(MAX_PARTICIPANTS - MIN_PARTICIPANTS) + MIN_PARTICIPANTS);
        when(tournamentRepository.findByTournamentKey(tournament.getTournamentKey())).thenReturn(Optional.of(tournament));


        //when
        Throwable throwable = catchThrowable(() -> tournamentService.setRegistrationStatus(tournament.getTournamentKey(), false));
        JsonResponse result = tournamentService.setRegistrationStatus(tournament.getTournamentKey(), false);


        //then
        assertThat(throwable).isNull();
        assertThat(tournament.isRegistrationOpen()).isEqualTo(false);
        assertThat(result.getResponse().get("status")).isEqualTo("ok");
        assertThat(result.getResponse().get("message")).isEqualTo(String.format("Registration status set to %b for tournament %s",
                tournament.isRegistrationOpen(), tournament.getName()));

    }

    @Test
    public void setRegistrationFailTournamentNotFound() {

        //given
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.CREATED, true,
                secureRandom.nextInt(MAX_PARTICIPANTS - MIN_PARTICIPANTS) + MIN_PARTICIPANTS);
        when(tournamentRepository.findByTournamentKey(tournament.getTournamentKey())).thenReturn(Optional.empty());


        //when
        Throwable throwable = catchThrowable(() -> tournamentService.setRegistrationStatus(tournament.getTournamentKey(), false));


        //then
        assertThat(throwable).isInstanceOf(HttpClientErrorException.class).hasMessage("404 Tournament not found");
    }

    @Test
    public void setRegistrationFailTournamentAlreadyActive() {

        //given
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.ACTIVE, false,
                secureRandom.nextInt(MAX_PARTICIPANTS - MIN_PARTICIPANTS) + MIN_PARTICIPANTS);
        when(tournamentRepository.findByTournamentKey(tournament.getTournamentKey())).thenReturn(Optional.of(tournament));


        //when
        Throwable throwable = catchThrowable(() -> tournamentService.setRegistrationStatus(tournament.getTournamentKey(), true));


        //then
        assertThat(throwable).isInstanceOf(HttpClientErrorException.class)
                .hasMessage("422 Cannot change registration status for a tournament that's ongoing or finished!");
    }

    @Test
    public void setRegistrationFailTournamentAlreadyEnded() {

        //given
        Tournament tournament = TestEntityGenerator.generateTournament(GameState.ENDED, false,
                secureRandom.nextInt(MAX_PARTICIPANTS - MIN_PARTICIPANTS) + MIN_PARTICIPANTS);
        when(tournamentRepository.findByTournamentKey(tournament.getTournamentKey())).thenReturn(Optional.of(tournament));


        //when
        Throwable throwable = catchThrowable(() -> tournamentService.setRegistrationStatus(tournament.getTournamentKey(), true));


        //then
        assertThat(throwable).isInstanceOf(HttpClientErrorException.class)
                .hasMessage("422 Cannot change registration status for a tournament that's ongoing or finished!");
    }

    @Test
    public void loadAllTournamentsSuccessfullyTypeAll() {

        //given
        List<Tournament> tournaments = new ArrayList<>();

        for (int noOfTournaments = TOURNAMENTS_AMOUNT; noOfTournaments > 0; noOfTournaments--) {
            if (noOfTournaments % 2 == 0) {
                tournaments.add(TestEntityGenerator.generateTournament(GameState.CREATED, true, MAX_PARTICIPANTS));
            } else if(noOfTournaments % 3 == 0) {
                tournaments.add(TestEntityGenerator.generateTournament(GameState.ACTIVE, true, MAX_PARTICIPANTS));
            } else {
                tournaments.add(TestEntityGenerator.generateTournament(GameState.ENDED, true, MAX_PARTICIPANTS));
            }
        }

        when(tournamentRepository.findAll()).thenReturn(tournaments);

        //when
        JsonResponse result = tournamentService.loadAllTournaments(TournamentStatus.ALL);

        //then
        assertThat(result.getResponse().get("status")).isEqualTo("ok");

        List<JsonResponse> resultList = (List<JsonResponse>) result.getResponse().get("tournaments");

        assertThat(resultList.size()).isEqualTo(tournaments.size());

        for (int tournamentNo = 0; tournamentNo < tournaments.size(); tournamentNo++) {
            assertThat(resultList.get(tournamentNo).getResponse().get("start_date"))
                    .isEqualTo(tournaments.get(tournamentNo).getSchedule().getStartDate().toString());
            assertThat(resultList.get(tournamentNo).getResponse().get("end_date"))
                    .isEqualTo(tournaments.get(tournamentNo).getSchedule().getEndDate().toString());
            assertThat(resultList.get(tournamentNo).getResponse().get("location"))
                    .isEqualTo(tournaments.get(tournamentNo).getSchedule().getLocation());
            assertThat(resultList.get(tournamentNo).getResponse().get("tournament_name"))
                    .isEqualTo(tournaments.get(tournamentNo).getName());
            assertThat(resultList.get(tournamentNo).getResponse().get("tournament_key"))
                    .isEqualTo(tournaments.get(tournamentNo).getTournamentKey());
            assertThat(resultList.get(tournamentNo).getResponse().get("tournament_status"))
                    .isEqualTo(tournaments.get(tournamentNo).getState().toString());
        }
    }

    @Test
    public void loadAllTournamentsSuccessfullyTypeCreated() {

        //given
        List<Tournament> tournaments = generateTournaments(GameState.CREATED);

        when(tournamentRepository.findAllByState(GameState.CREATED)).thenReturn(tournaments);

        //when
        JsonResponse result = tournamentService.loadAllTournaments(TournamentStatus.CREATED);

        //then
        assertThat(result.getResponse().get("status")).isEqualTo("ok");

        List<JsonResponse> resultList = (List<JsonResponse>) result.getResponse().get("tournaments");

        for (int tournamentNo = 0; tournamentNo < tournaments.size(); tournamentNo++) {
            assertThat(resultList.get(tournamentNo).getResponse().get("tournament_name"))
                    .isEqualTo(tournaments.get(tournamentNo).getName());
            assertThat(resultList.get(tournamentNo).getResponse().get("tournament_key"))
                    .isEqualTo(tournaments.get(tournamentNo).getTournamentKey());
            assertThat(resultList.get(tournamentNo).getResponse().get("tournament_status"))
                    .isEqualTo(tournaments.get(tournamentNo).getState().toString());
        }
    }

    @Test
    public void loadAllTournamentsSuccessfullyTypeActive() {

        //given
        List<Tournament> tournaments = generateTournaments(GameState.ACTIVE);

        when(tournamentRepository.findAllByState(GameState.ACTIVE)).thenReturn(tournaments);

        //when
        JsonResponse result = tournamentService.loadAllTournaments(TournamentStatus.ACTIVE);

        //then
        assertThat(result.getResponse().get("status")).isEqualTo("ok");

        List<JsonResponse> resultList = (List<JsonResponse>) result.getResponse().get("tournaments");

        for (int tournamentNo = 0; tournamentNo < tournaments.size(); tournamentNo++) {
            assertThat(resultList.get(tournamentNo).getResponse().get("tournament_name"))
                    .isEqualTo(tournaments.get(tournamentNo).getName());
            assertThat(resultList.get(tournamentNo).getResponse().get("tournament_key"))
                    .isEqualTo(tournaments.get(tournamentNo).getTournamentKey());
            assertThat(resultList.get(tournamentNo).getResponse().get("tournament_status"))
                    .isEqualTo(tournaments.get(tournamentNo).getState().toString());
        }
    }

    @Test
    public void loadAllTournamentsSuccessfullyTypeEnded() {

        //given
        List<Tournament> tournaments = generateTournaments(GameState.ENDED);

        when(tournamentRepository.findAllByState(GameState.ENDED)).thenReturn(tournaments);

        //when
        JsonResponse result = tournamentService.loadAllTournaments(TournamentStatus.ENDED);

        //then
        assertThat(result.getResponse().get("status")).isEqualTo("ok");

        List<JsonResponse> resultList = (List<JsonResponse>) result.getResponse().get("tournaments");

        for (int tournamentNo = 0; tournamentNo < tournaments.size(); tournamentNo++) {
            assertThat(resultList.get(tournamentNo).getResponse().get("tournament_name"))
                    .isEqualTo(tournaments.get(tournamentNo).getName());
            assertThat(resultList.get(tournamentNo).getResponse().get("tournament_key"))
                    .isEqualTo(tournaments.get(tournamentNo).getTournamentKey());
            assertThat(resultList.get(tournamentNo).getResponse().get("tournament_status"))
                    .isEqualTo(tournaments.get(tournamentNo).getState().toString());
        }
    }

    private List<Tournament> generateTournaments(GameState state) {
        List<Tournament> tournaments = new ArrayList<>();

        for (int tournamentCount = TOURNAMENTS_AMOUNT; tournamentCount > 0; tournamentCount--) {
            tournaments.add(TestEntityGenerator.generateTournament(state, true, MAX_PARTICIPANTS));
        }

        return tournaments;
    }
}
