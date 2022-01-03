package com.api.service;

import com.api.entities.*;
import com.api.env.resources.AppResources;
import com.api.mapper.*;
import com.api.model.*;
import com.api.output.*;
import com.api.repository.*;
import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.email.model.Template;
import com.email.service.EmailManager;
import com.util.enums.PlatformType;
import com.util.web.JsonResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import javax.inject.Inject;
import javax.persistence.criteria.Join;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.data.jpa.domain.Specification.*;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class TournamentService {

    private final static String GHOST_PLAYER_EMAIL = "GHOST_EMAIL_";

    private final TournamentRepository tournamentRepository;
    private final EmailManager emailManager;
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final RoundRepository roundRepository;
    private final MatchResultRepository matchResultRepository;
    private final TournamentUserRepository tournamentUserRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final UserRepository userRepository;
    private final UserPlatformRepository userPlatformRepository;
    private final ResultService resultService;

    @Transactional
    public TournamentJSON addTournament(final TournamentInput tournamentInput) {

        final Tournament tournament = TournamentMapper.inputToTournament(tournamentInput);
        tournament.setSchedule(ScheduleMapper.inputToSchedule(tournamentInput.getSchedule()));
        tournament.setTournamentKey(NanoIdUtils.randomNanoId());

        tournamentRepository.save(tournament);
        return TournamentMapper.tournamentToJSON(tournament);
    }

    /**
     * Sends an email invitation to a player to participate at the given tournament
     *
     * @param tournamentKey the key of the tournament
     * @param userKey       the key of the user
     * @return the JSON of the user
     */

    @Transactional
    public JsonResponse invitePlayer(String tournamentKey, String userKey) {

        final Tournament tournament = tournamentRepository.findByTournamentKey(tournamentKey)
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Tournament not found!"));

        final Player player = playerRepository.findByUserKey(userKey)
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Player not found!"));

        if (!isRegistrationAvailable(tournament)) {
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "Registrations for this tournament are closed!");
        }

        Optional<UserSettings> userSettingsOptional = userSettingsRepository.findByPlayerAndTournament(player, tournament);

        if (userSettingsOptional.isPresent() && isExpired(userSettingsOptional.get())) {
            userSettingsOptional.get().setActive(false);
            player.getUserSettings().remove(userSettingsOptional.get());
        }

        if (!userSettingsOptional.isPresent() || !userSettingsOptional.get().isActive()) {
            UserSettings userSettings = generateUserSettings(player, tournament);
            player.getUserSettings().add(userSettings);

            sendEmail(player, tournament, Template.PARTICIPATE_HTML);

            return new JsonResponse().with("status", "ok")
                    .with("message", "Player invited to the tournament!")
                    .done();
        } else {
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "Player already has an active invitation to this tournament!");
        }
    }

    private static boolean isExpired(UserSettings userSettings) {
        return userSettings.getExpirationDate().isBefore(LocalDateTime.now());
    }

    /**
     * After a player confirms his participation at a tournament, this method
     * registers him to that tournament, if the tournament is already started or has ended
     * and apology email gets sent, otherwise after adding the player to the tournament
     * an email gets sent with the confirmation
     *
     * @param userKey       the encrypted key of an user
     * @param tournamentKey the encrypted key of a tournament
     * @return the JSON of a player
     */

    @Transactional
    public JsonResponse registerPlayer(String userKey, String tournamentKey) {

        Player player = playerRepository.findByUserKey(userKey)
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Player not found!"));

        Tournament tournament = tournamentRepository.findByTournamentKey(tournamentKey)
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Tournament not found!"));

        if (isRegistrationAvailable(tournament)) {
            addPlayerToTournament(player, tournament);

        } else {
            sendEmail(player, tournament, Template.APOLOGY_HTML);
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "Tournament participation is disabled!");
        }

        return new JsonResponse().with("status", "ok")
                .with("userKey", userKey)
                .done();
    }

    /**
     * Adds the player to the tournament that he/she wants to participate in, if he already
     * confirmed his participation, an exception is thrown.
     *
     * @param player     the player that confirmed his participation
     * @param tournament the tournament that the player wants to participate in
     */

    @Transactional
    public void addPlayerToTournament(Player player, Tournament tournament) {
        player.setScore(0);

        if (tournament.addPlayer(player)) {
            tournamentRepository.save(tournament);

            sendEmail(player, tournament, Template.SUCCESS_HTML);

            UserSettings userSettings = userSettingsRepository.findByPlayerAndTournament(player, tournament)
                    .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "INVITATION FOR USER NOT FOUND"));
            player.getUserSettings().remove(userSettings);
            userSettingsRepository.deleteAllByPlayerAndTournament(player, tournament);

        } else {
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Player is already participating in the tournament!");
        }
    }

    /**
     * This methods sets a tournament's status to ACTIVE, then sends a notification through the WebSocket
     * to all connected clients stating the tournament has started, updates the tournament in DB and then proceeds
     * to generated the rounds
     *
     * @param tournamentKey the key of the tournament to start
     */

    @Transactional
    public void startTournament(String tournamentKey) {
        Tournament tournament = tournamentRepository.findByTournamentKey(tournamentKey)
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Tournament not found"));

        if (GameState.ACTIVE.equals(tournament.getState())) {
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "Tournament has already started!");
        } else if (GameState.ENDED.equals(tournament.getState())) {
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "Tournament has already ended!");
        }

        generateRounds(tournament);

        tournament.setRegistrationOpen(false);

        tournament.setState(GameState.ACTIVE);

        sendNotification("Tournament started!");

    }

    /**
     * Sets the given tournament's status to ENDED
     *
     * @param tournamentKey the key of the tournament to start
     */

    @Transactional
    public JsonResponse endTournament(String tournamentKey) {
        Tournament tournament = tournamentRepository.findByTournamentKey(tournamentKey)
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Tournament not found"));

        if (GameState.ENDED.equals(tournament.getState())) {
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "Tournament has already ended!");
        } else if (GameState.CREATED.equals(tournament.getState())) {
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "Tournament did not start yet!");
        }

        tournament.setRegistrationOpen(false);
        tournament.setState(GameState.ENDED);

        sendNotification("Tournament '" + tournament.getName() + "' ended! The winner is: "
                + getTournamentWinner(tournament).getFullName());

        return new JsonResponse().with("status", "ok")
                .with("message", "Tournament ended.");
    }

    /**
     * This method generates rounds and matches for a given tournament according to the
     * Round Robin algorithm. If the player count is odd
     * then a ghost player gets generated against which a player always wins.
     * The algorithm shifts players between lists, clockwise, starting from the second player,
     * so that each player will play against each other. The number of rounds generated
     * according to RR algorithm is n = players size - 1
     *
     * @param tournament the tournament for which the rounds need to be generated
     */

    private void generateRounds(Tournament tournament) {
        List<Player> players = tournament.getPlayers();

        if (players.size() <= 0) {
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "This tournament currently has no participants!");
        } else {
            if (players.size() % 2 != 0) {
                players.add(generateGhost(tournament.getTournamentKey()));
            }
            int playersCount = players.size();
            int totalRounds = playersCount - 1;

            LinkedList<Player> firstHalf = new LinkedList<>(players.subList(0, players.size() / 2));
            LinkedList<Player> secondHalf = new LinkedList<>(players.subList(players.size() / 2, players.size()));

            Map<Integer, Round> roundsMap = new HashMap<>();
            for (int roundIndex = 0; roundIndex < totalRounds; roundIndex++) {

                Round currentRound = generateRound(tournament, roundIndex);
                roundsMap.put(currentRound.getNumber(), currentRound);

                if (roundIndex == 0) {
                    tournament.setCurrentRound(currentRound);
                }

                List<Match> matchList = new ArrayList<>();
                for (int matchIndex = 0; matchIndex < playersCount / 2; matchIndex++) {

                    Match currentMatch = generateMatch(currentRound, tournament);
                    matchList.add(currentMatch);

                    Player firstPlayer = firstHalf.get(matchIndex);
                    Player secondPlayer = secondHalf.get(secondHalf.size() - 1 - matchIndex);

                    generateMatchPlayer(tournament, currentMatch, firstPlayer, secondPlayer);
                    checkGhostPlayer(firstPlayer, secondPlayer, currentMatch);
                }
                currentRound.setMatches(matchList);

                firstHalf.add(1, secondHalf.getLast());
                secondHalf.addFirst(firstHalf.getLast());

                firstHalf.removeLast();
                secondHalf.removeLast();
            }
            tournament.setRounds(roundsMap);
        }
    }

    /**
     * This method is used by the controller to return a list of round JSONs after generating the rounds.
     *
     * @param tournamentKey the key of the tournament
     * @return returns a list of JSONs with the rounds of a tournament
     */

    public List<RoundJSON> getGeneratedRounds(String tournamentKey) {
        Tournament tournament = tournamentRepository.findByTournamentKey(tournamentKey)
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Tournament not found"));

        return roundRepository.findAllByTournament(tournament)
                .stream()
                .map(RoundMapper::entityToJSON)
                .collect(Collectors.toList());
    }

    /**
     * This method checks if one of the players is a Ghost, if so it generated the match result
     * because a player will always win against a Ghost player.
     *
     * @param firstPlayer  the entity containing the first player
     * @param secondPlayer the entity containing the second player
     * @param currentMatch the match that is currently being generated
     */

    @Transactional
    public void checkGhostPlayer(Player firstPlayer, Player secondPlayer, Match currentMatch) {
        String ghostEmail = GHOST_PLAYER_EMAIL + currentMatch.getTournament().getTournamentKey();

        if (ghostEmail.equals(firstPlayer.getEmail())) {
            generateMatchResult(firstPlayer, secondPlayer, currentMatch, Result.SECOND);
            resultService.applyResultForGhost(secondPlayer.getId(), currentMatch.getTournament().getId());
            currentMatch.setState(GameState.ENDED);

        } else if (ghostEmail.equals(secondPlayer.getEmail())) {
            generateMatchResult(firstPlayer, secondPlayer, currentMatch, Result.FIRST);
            resultService.applyResultForGhost(firstPlayer.getId(), currentMatch.getTournament().getId());
            currentMatch.setState(GameState.ENDED);
        } else {
            generateMatchResult(firstPlayer, secondPlayer, currentMatch, null);
        }
    }

    /**
     * This method sends an email to a user based on one of the HTML templates from resources/mail/html.
     *
     * @param user          the user that receives the email
     * @param tournament    the tournament related to the email
     * @param emailTemplate the HTML template of the email
     */

    public void sendEmail(User user,
                          Tournament tournament,
                          Template emailTemplate) {
        String avgLogoUrl = "https://i.imgur.com/td7W7oT.png";
        String chessLogoUrl = "https://i.imgur.com/GpScRpX.png";
        String url = "";

        Map<String, Object> templateVariables = new HashMap<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        String startDate = tournament.getSchedule().getStartDate().format(formatter);
        String endDate = tournament.getSchedule().getEndDate().format(formatter);

        templateVariables.put("firstname", user.getFirstName());
        templateVariables.put("tournamentName", tournament.getName());
        templateVariables.put("avgLogo", avgLogoUrl);
        templateVariables.put("chessLogo", chessLogoUrl);
        templateVariables.put("startDate", startDate);
        templateVariables.put("endDate", endDate);
        templateVariables.put("location", tournament.getSchedule().getLocation());

        switch (emailTemplate) {
            case PARTICIPATE_HTML:
                url = AppResources.TOURNAMENT_REGISTER_CONFIRM_URL.value() + user.getUserKey() + "/" + tournament.getTournamentKey();
                break;

            case APOLOGY_HTML:
                url = AppResources.TOURNAMENT_URL.value();
                break;

            case SUCCESS_HTML:
                url = AppResources.CONFIRMATION_SUCCESS_URL.value();
                break;
        }

        templateVariables.put("confirmationLink", url);

        emailManager.send(
                user.getEmail(),
                "Chess Tournament Participation",
                emailTemplate,
                templateVariables,
                Locale.ENGLISH);
    }

    /**
     * This method generates a Ghost player. The ghost player automatically loses when playing against other players.
     * Is only called when a tournament's number of participants is odd.
     *
     * @param tournamentKey the key of the tournament
     * @return returns an entity containing a Ghost player
     */
    public Player generateGhost(String tournamentKey) {
        Player ghostPlayer = Player.builder()
                .score(-1)
                .email(GHOST_PLAYER_EMAIL + tournamentKey)
                .build();

        playerRepository.save(ghostPlayer);

        return ghostPlayer;
    }

    /**
     * This method sends a message through the WebSocket to all connected clients.
     *
     * @param message the message to send
     */
    public static void sendNotification(String message) {
        WebSocketManager.sendMessage(message);
    }

    public MatchResult generateMatchResult(Player firstPlayer,
                                           Player secondPlayer,
                                           Match currentMatch,
                                           Result result) {

        MatchResult matchResult = MatchResult.builder()
                .result(result)
                .firstPlayer(firstPlayer)
                .secondPlayer(secondPlayer)
                .build();

        matchResultRepository.save(matchResult);
        currentMatch.setMatchResult(matchResult);
        matchResult.setMatchResultKey(NanoIdUtils.randomNanoId());

        return matchResult;
    }

    public Round generateRound(Tournament tournament, int roundIndex) {
        GameState state = roundIndex == 0 ? GameState.ACTIVE : GameState.CREATED;

        Round currentRound = Round.builder()
                .state(state)
                .tournament(tournament)
                .number(roundIndex + 1)
                .build();

        roundRepository.save(currentRound);
        currentRound.setRoundKey(NanoIdUtils.randomNanoId());

        return currentRound;
    }

    public Match generateMatch(Round round, Tournament tournament) {
        GameState state = round.getNumber() == 1 ? GameState.ACTIVE : GameState.CREATED;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String localDateTime = LocalDateTime.now().format(formatter);

        Match currentMatch = Match.builder()
                .round(round)
                .tournament(tournament)
                .state(state)
                .startDate(LocalDateTime.parse(localDateTime, formatter))
                .build();

        matchRepository.save(currentMatch);
        currentMatch.setMatchKey(NanoIdUtils.randomNanoId());

        return currentMatch;
    }

    public MatchPlayer generateMatchPlayer(Tournament tournament,
                                           Match match,
                                           Player firstPlayer, Player secondPlayer) {

        MatchPlayer matchPlayer = new MatchPlayer(match, firstPlayer, secondPlayer, tournament);
        matchPlayer.setMatchPlayerKey(NanoIdUtils.randomNanoId());

        matchPlayerRepository.save(matchPlayer);
        return matchPlayer;
    }

    @Transactional
    public ChessTournamentJSON getTournament(String tournamentKey) {

        Tournament tournament = tournamentRepository.findByTournamentKey(tournamentKey)
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Tournament not found"));

        sendTournamentStatus(tournament);

        return ChessTournamentMapper.entityToJSON(tournament);
    }

    public static void sendTournamentStatus(Tournament tournament) {
        WebSocketManager.sendTournamentInformation(tournament);
    }

    @Transactional
    public List<PlayerJSON> getOverallLeaderboard() {
        return playerRepository.findAllByOrderByScoreDesc();
    }


    @Transactional
    public List<PlayerJSON> getTemporaryLeaderboard(String tournamentKey) {
        Tournament tournament = tournamentRepository.findByTournamentKey(tournamentKey)
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Tournament not found!"));
        List<TournamentUser> tournamentPlayers = tournamentUserRepository.findByTournamentOrderByScoreDesc(tournament);

        return tournamentPlayers.stream()
                .filter(player -> player.getTournamentUserId().getTournamentId() == tournament.getId()
                        && !player.getUser().getEmail().contains(GHOST_PLAYER_EMAIL))
                .map(player -> {
                    PlayerJSON playerJSON = PlayerMapper.userToPlayerJson(player.getUser());
                    playerJSON.setScore(player.getScore());
                    return playerJSON;
                })
                .collect(Collectors.toList());
    }

    private User getTournamentWinner(Tournament tournament) {
        return tournamentUserRepository.findTopByTournamentOrderByScoreDesc(tournament)
                .orElseThrow(() ->
                        new HttpClientErrorException(HttpStatus.NOT_FOUND,
                                "Tournament Player relationship not found!")).getUser();
    }


    public RoundJSON getCurrentRound(String tournamentKey) {
        final Tournament tournament = tournamentRepository.findByTournamentKey(tournamentKey)
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Tournament not found!"));

        return RoundMapper.entityToJSON(tournament.getCurrentRound());
    }

    /***
     * Takes a given tournament and sends a WebSocket notification with the number of the current round, to the tournament players.
     *
     * @param tournament the tournament to send notification for
     * @return returns a JsonResponse containing the current round number
     */
    public JsonResponse roundNotification(Tournament tournament) {

        Round currentRound = tournament.getCurrentRound();
        if (tournament.getRounds() == null
                || currentRound == null) {
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Tournament hasn't started yet!");
        }
        sendNotification("Round " + currentRound.getNumber() + " started!");
        return new JsonResponse().with("status", "ok")
                .with("message", "Round " + currentRound.getNumber() + " started!")
                .done();
    }

    @Transactional
    public RoundJSON getRound(String tournamentKey, String roundKey) {
        Tournament tournament = tournamentRepository.findByTournamentKey(tournamentKey)
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Tournament not found"));

        Round round = roundRepository.findByRoundKey(roundKey)
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Round not found"));

        if (tournament.getRounds().containsValue(round)) {
            return RoundMapper.entityToJSON(round);

        } else {
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "No round with key " + roundKey + " in given tournament");
        }

    }

    /***
     * Takes a given tournament and checks if the current round has been set for it.
     * If the tournament's current round is the final round of the tournament,
     * then the round's last match state will be set to 'ENDED' , the round's state will be set to 'ENDED'
     * and the tournament's state will be set to 'ENDED'.
     * If the current round is not final, it will switch to the next round and set its matches state to 'ACTIVE'
     * and notify the players that the next round started.
     *
     * @param tournamentKey the key of the tournament to switch rounds for
     * @return returns the next round of the given tournament in JSON format
     */
    @Transactional
    public RoundJSON switchToNextRound(String tournamentKey) {
        Tournament tournament = tournamentRepository.findByTournamentKey(tournamentKey)
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Tournament not found"));

        if (GameState.CREATED.equals(tournament.getState()) ||
                tournament.getCurrentRound() == null) {
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "Tournament did not start yet!");
        }

        Round currentRound = tournament.getCurrentRound();

        if (isFinalRound(tournament, currentRound)) {
            currentRound.getMatches().get(currentRound.getMatches().size() - 1).setState(GameState.ENDED);
            currentRound.setState(GameState.ENDED);
            tournament.setState(GameState.ENDED);
            return RoundMapper.entityToJSON(currentRound);
        }

        Round nextRound = tournament.getRounds().get(currentRound.getNumber() + 1);
        boolean canMoveToNextRound = nextRound != null && (GameState.ACTIVE.equals(tournament.getState()) && roundFinished(currentRound));

        if (canMoveToNextRound) {
            switchRounds(tournament, currentRound, nextRound);
            startRound(nextRound);
            roundNotification(tournament);
            return RoundMapper.entityToJSON(nextRound);
        }

        throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "Round is still ongoing!");
    }

    /***
     * Takes a tournament key and round key and sets the round as the tournament's current round.
     * If the round is already set as the current round of the tournament, it will throw a HttpClientErrorException with code 422.
     *
     * @param tournamentKey the key of the tournament
     * @param roundKey the key of the round
     * @return the round that has been set as the current round, in JSON format
     */
    public RoundJSON setRound(String tournamentKey, String roundKey) {
        Tournament tournament = tournamentRepository.findByTournamentKey(tournamentKey)
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Tournament not found"));

        Round round = roundRepository.findByRoundKey(roundKey)
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Round not found"));

        if (isCurrentRound(tournament, round)) {
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "Given round is already set as the current round!");
        }

        round.setState(GameState.ACTIVE);

        tournament.setCurrentRound(round);
        startRound(round);
        sendNotification("Round " + round.getNumber() + " is about to start!");

        return RoundMapper.entityToJSON(round);
    }

    /***
     * Takes a given round and iterates over its matches, filtering the matches that ended
     * and compares the total size of the matches list, with the number of matches that ended.
     *
     * @param round the round to verify
     * @return returns a boolean result of the comparison between the two total counts
     */
    public boolean roundFinished(Round round) {
        int finishedMatches = (int) round.getMatches()
                .stream()
                .filter(match -> GameState.ENDED.equals(match.getState()))
                .count();

        return round.getMatches().size() == finishedMatches;
    }


    /***
     * Takes a given tournament and 2 rounds belonging to it, and changes the tournament's current round
     * from the current round to the next one.
     *
     * @param tournament the tournament that contains both rounds
     * @param currentRound the current round of the tournament
     * @param nextRound the new round
     */
    @Transactional
    public void switchRounds(Tournament tournament,
                             Round currentRound,
                             Round nextRound) {

        currentRound.setState(GameState.ENDED);
        tournament.setCurrentRound(nextRound);
        nextRound.setState(GameState.ACTIVE);

        startRound(nextRound);
    }

    /***
     * Takes a given round's list of matches, and sets their state to 'ACTIVE' for all of them.
     *
     * @param round the round with the matches that will be started
     */
    public void startRound(Round round) {
        List<Match> activeMatches = round.getMatches().stream()
                .map(match -> {
                    match.setState(GameState.ACTIVE);
                    matchRepository.save(match);
                    return match;
                })
                .collect(Collectors.toList());
    }

    /***
     * Takes a given tournament and round, to check if the given round number is equal to the tournament's number of rounds,
     * meaning that it is the final round of the tournament.
     *
     * @param tournament the tournament that has a Map of rounds
     * @param round the round to check
     * @return boolean result
     */
    public boolean isFinalRound(Tournament tournament, Round round) {
        if (tournament.getRounds().values().size() == round.getNumber()) {
            return true;
        } else {
            return false;
        }
    }

    @Transactional
    public MatchJSON getLastPlayedMatch(String tournamentKey) {
        Tournament tournament = tournamentRepository.findByTournamentKey(tournamentKey)
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Tournament not found"));

        Match match = matchRepository.findTopByTournamentAndStateOrderByStartDateDesc(tournament, GameState.ENDED)
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Match not found!"));

        return MatchMapper.entityToJSON(match);
    }

    @Transactional
    public List<MatchResultJSON> getPlayerResults(String tournamentKey, String playerKey) {
        Tournament tournament = tournamentRepository.findByTournamentKey(tournamentKey)
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Tournament not found!"));

        Player player = playerRepository.findByUserKey(playerKey)
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Player not found!"));

        List<MatchPlayer> matchPlayerList = tournament.getMatchPlayerPairs();
        List<Match> matchList = matchPlayerList.stream()
                .filter(matchPlayer -> player.equals(matchPlayer.getFirstPlayer())
                        || player.equals(matchPlayer.getSecondPlayer()))
                .flatMap(matchPlayer -> Stream.of(matchPlayer.getMatch()))
                .collect(Collectors.toList());

        return matchList.stream().flatMap(match -> Stream.of(match.getMatchResult()))
                .map(MatchResultMapper::entityToJSON)
                .collect(Collectors.toList());
    }

    /***
     * Takes a tournament key and a boolean value and looks for the tournament with the given key.
     * If the tournament's state is 'ACTIVE' or 'ENDED' the method will throw a HttpClientErrorException with code 422
     * If the tournament's state is 'CREATED' the method will change registration status for the tournament with the given boolean.
     *
     * @param tournamentKey the key of the tournament
     * @param registrationStatus the registration status for the tournament
     * @return returns a JsonResponse with the tournament name and registration status
     */
    @Transactional
    public JsonResponse setRegistrationStatus(String tournamentKey, boolean registrationStatus) {
        Tournament tournament = tournamentRepository.findByTournamentKey(tournamentKey)
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Tournament not found"));

        if (!GameState.ACTIVE.equals(tournament.getState())
                && !GameState.ENDED.equals(tournament.getState())) {
            tournament.setRegistrationOpen(registrationStatus);
            tournamentRepository.save(tournament);
        } else {
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "Cannot change registration status for a tournament that's ongoing or finished!");
        }

        String message = String.format("Registration status set to %b for tournament %s",
                registrationStatus, tournament.getName());

        return new JsonResponse().with("status", "ok")
                .with("message", message)
                .done();
    }

    /***
     * Takes a tournament key and an integer number of participants and looks for the tournament with the given key.
     * If the tournament's state is 'ACTIVE' or 'ENDED' the method will throw an error to inform that the number of maximum participants cannot be changed anymore.
     * If the tournament's state is 'CREATED' the method will change the number of maximum participants for the tournament with the given integer.
     *
     * @param tournamentKey the key of the tournament
     * @param maxParticipants the maximum number of participants for the tournament
     * @return returns a JsonResponse with the tournament name and maximum participants
     */
    @Transactional
    public JsonResponse setMaxParticipants(String tournamentKey, int maxParticipants) {
        Tournament tournament = tournamentRepository.findByTournamentKey(tournamentKey)
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Tournament not found"));

        if (!GameState.ACTIVE.equals(tournament.getState())
                && !GameState.ENDED.equals(tournament.getState())) {
            tournament.setMaxParticipants(maxParticipants);
            tournamentRepository.save(tournament);
        } else {
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "Cannot change number of participants for a tournament that's ongoing or finished!");
        }

        String message = String.format("Maximum number of participants set to %d for tournament %s",
                maxParticipants, tournament.getName());

        return new JsonResponse().with("status", "ok")
                .with("message", message)
                .done();
    }

    /***
     * Takes a given tournament and compares the size of the current participants' list to the number of maximum participants, currently set for the tournament.
     *
     * @param tournament the tournament to check registration status for
     * @return returns a boolean
     */
    private static boolean isRegistrationAvailable(Tournament tournament) {
        return tournament.isRegistrationOpen()
                && tournament.getPlayers().size() < tournament.getMaxParticipants();
    }

    @Transactional
    public RoundJSON getActiveRound(String tournamentKey) {
        Tournament tournament = tournamentRepository.findByTournamentKey(tournamentKey)
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Tournament not found!"));

        return RoundMapper.entityToJSON(tournament.getCurrentRound());
    }

    public UserSettings generateUserSettings(Player player, Tournament tournament) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String createdDate = LocalDateTime.now().format(formatter);

        boolean isActive = true;

        LocalDateTime expirationDate = LocalDateTime.now().plusHours(4);

        if (expirationDate.isAfter(tournament.getSchedule().getStartDate())) {
            expirationDate = tournament.getSchedule().getStartDate();
        }

        if (expirationDate.isBefore(LocalDateTime.now())) {
            isActive = false;
        }

        return UserSettings.builder()
                .active(isActive)
                .player(player)
                .createdDate(LocalDateTime.parse(createdDate, formatter))
                .expirationDate(expirationDate)
                .tournament(tournament)
                .type(Type.TOURNAMENT_INVITATION)
                .build();
    }

    @Transactional
    public List<ParticipantStatusJSON> listAllParticipants(String tournamentKey) {
        Tournament tournament = tournamentRepository.findByTournamentKey(tournamentKey)
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Tournament not found"));

        List<TournamentUser> tournamentUsers = tournamentUserRepository.findUserByTournament(tournament);

        List<ParticipantStatusJSON> participants = tournamentUsers.stream()
                .filter(tournamentUser -> !tournamentUser.getUser().getEmail().contains(GHOST_PLAYER_EMAIL))
                .map(tournamentUser -> ParticipantStatusMapper.createJSON(tournamentUser.getUser(), InvitationStatus.ACCEPTED))
                .collect(Collectors.toList());

        List<UserSettings> playersInvited = userSettingsRepository.findByTournament(tournament);

        List<ParticipantStatusJSON> invitedUsers = playersInvited.stream()
                .map(userSettings -> ParticipantStatusMapper.createJSON(userSettings.getPlayer(), InvitationStatus.PENDING))
                .collect(Collectors.toList());

        return Stream.concat(participants.stream(), invitedUsers.stream()).collect(Collectors.toList());
    }

    @Transactional
    public JsonResponse assignRefereeToTournament(String refereeKey, String tournamentKey) {

        Tournament tournament = tournamentRepository.findByTournamentKey(tournamentKey).orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Tournament not found!"));
        User referee = userRepository.findByUserKey(refereeKey).orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "User not found!"));

        if (!tournament.getReferees().contains(referee)) {
            tournament.addReferee(referee);
        } else {
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "Referee is already added to this tournament!");
        }

        return new JsonResponse().with("status", "ok")
                .with("message", "Referee added in tournament!")
                .done();
    }

    @Transactional
    public long getTotalNumberOfOngoingMatches(String tournamentKey) {

        Tournament tournament = tournamentRepository.findByTournamentKey(tournamentKey)
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Tournament not found"));

        return matchRepository.countByTournamentAndState(tournament, GameState.ACTIVE);
    }

    public JsonResponse loadAllTournaments(TournamentStatus status) {
        List<Tournament> tournaments = new ArrayList<>();

        if (TournamentStatus.ALL.equals(status)) {
            tournaments = tournamentRepository.findAll();
        } else if (EnumUtils.isValidEnum(TournamentStatus.class, status.toString())) {
            tournaments = tournamentRepository.findAllByState(GameState.valueOf(status.toString()));
        }

        JsonResponse response = new JsonResponse().with("status", "ok");

        List<JsonResponse> tournamentJsons = tournaments.stream()
                .map(tournament -> new JsonResponse()
                        .with("start_date", tournament.getSchedule().getStartDate().toString())
                        .with("end_date", tournament.getSchedule().getEndDate().toString())
                        .with("location", tournament.getSchedule().getLocation())
                        .with("tournament_name", tournament.getName())
                        .with("tournament_key", tournament.getTournamentKey())
                        .with("tournament_status", tournament.getState().toString()))
                .collect(Collectors.toList());

        response.with("tournaments", tournamentJsons);

        return response;
    }

    /***
     * Takes a given tournament and round and checks to see if the tournament's current round
     * is the same as the given round.
     *
     * @param tournament the tournament where the current round will be taken from
     * @param round the round to compare with
     * @return returns the boolean result of the comparison between the 2 rounds
     */
    public boolean isCurrentRound(Tournament tournament, Round round) {
        return tournament.getCurrentRound().equals(round);
    }

    public List<SearchParticipantsJSON> searchParticipants(ParticipantSearchCriteria participantSearchCriteria) {
        List<SearchParticipantsJSON> playerList;

        if (participantSearchCriteria.isEmpty()) {
            playerList = playerRepository.findAll().stream().map(PlayerMapper::playerToSearchParticipantsJSON).collect(Collectors.toList());
        } else {
            playerList = playerRepository.searchPlayers(participantSearchCriteria);
        }

        return playerList;
    }

    public List<SearchTournamentsJSON> searchTournaments(TournamentSearchCriteria tournamentSearchCriteria) {
        List<SearchTournamentsJSON> tournamentList;

        if (tournamentSearchCriteria.isEmpty()) {
            tournamentList = tournamentRepository.findAll().stream().map(TournamentMapper::tournamentToSearchTournamentsJSON).collect(Collectors.toList());
        } else {
            tournamentList = tournamentRepository.searchTournaments(tournamentSearchCriteria);
        }

        return tournamentList;
    }

    public List<SearchRefereeJSON> searchReferee(RefereeSearchCriteria refereeSearchCriteria) {
        List<SearchRefereeJSON> refereeList;

        if (refereeSearchCriteria.isEmpty()) {
            refereeList = userRepository.findAllReferees();
        } else {
            refereeList = playerRepository.searchReferee(refereeSearchCriteria);
        }

        return refereeList;
    }


    @Transactional
    public JsonResponse deleteTournament(final String tournamentKey) {
        final Tournament tournament = tournamentRepository.findByTournamentKey(tournamentKey)
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Tournament not found!"));

        tournament.getReferees().forEach(referee -> referee.getTournaments().remove(tournament));
        tournament.setMatchPlayerPairs(new ArrayList<>());

        List<Player> playersFound = playerRepository.findAll(where(isInTournament(tournament.getId()).and(isNotInUserSettings())));
        List<String> userKeys = new ArrayList<>();

        playersFound.forEach(player -> {
            AtomicBoolean canBeDeleted = new AtomicBoolean(true);

            player.getUserPlatformList().forEach(userPlatform -> {
                if (PlatformType.ADMIN.equals(userPlatform.getPlatformType())
                        || PlatformType.REFEREE.equals(userPlatform.getPlatformType())
                        || PlatformType.SUPER_ADMIN.equals(userPlatform.getPlatformType())) {
                    canBeDeleted.set(false);
                }
            });

            if (canBeDeleted.get()) {
                userKeys.add(player.getUserKey());
            }
        });

        tournamentUserRepository.deleteAllByTournament(tournament);
        matchPlayerRepository.deleteAllByTournament(tournament);
        matchRepository.deleteAllByTournament(tournament);
        roundRepository.deleteAllByTournament(tournament);
        userSettingsRepository.deleteAllByTournament(tournament);

        playerRepository.deletePlayerByUserKeyIn(userKeys);
        tournamentRepository.delete(tournament);

        return new JsonResponse().with("status", "ok")
                .with("message", "Tournament and associated relations has been deleted.");
    }

    private Specification<Player> isInTournament(int tournamentId) {
        return (playerRoot, criteriaQuery, builder) -> {
            Join<Player, TournamentUser> join = playerRoot.join("tournamentUsers");

            return builder.equal(join.get("tournament").get("id"), tournamentId);
        };
    }

    private Specification<Player> isNotInUserSettings() {
        return (playerRoot, criteriaQuery, builder) -> builder.isEmpty(playerRoot.get("userSettings"));
    }
}
