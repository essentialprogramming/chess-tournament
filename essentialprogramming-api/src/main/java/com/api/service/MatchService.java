package com.api.service;

import com.api.entities.*;
import com.api.mapper.MatchMapper;
import com.api.model.GameState;
import com.api.model.MatchSearchCriteria;
import com.api.model.Result;
import com.api.output.*;
import com.api.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.util.web.JsonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import javax.inject.Inject;
import javax.persistence.criteria.Join;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.springframework.data.jpa.domain.Specification.where;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class MatchService {

    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final TournamentUserRepository tournamentUserRepository;
    private final RoundRepository roundRepository;
    private final TournamentService tournamentService;
    private final UserRepository userRepository;
    private final ResultService resultService;

    @Transactional
    public MatchJSON reportMatchByPlayer(String userKey, String matchKey, String resultString) throws GeneralSecurityException, JsonProcessingException {
        Match currentMatch = matchRepository.findByMatchKey(matchKey).orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Match not found!"));
        if (currentMatch.getState() == GameState.ENDED) {
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "This match ended, cannot report a new result!");
        }

        Player player = playerRepository.findByUserKey(userKey).orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Player not found!"));
        MatchResult matchResult = currentMatch.getMatchResult();
        Result result = convertToResult(resultString);

        if (player.getId() == matchResult.getFirstPlayer().getId()) {
            if (matchResult.getFirstPlayerResult() == null) {
                matchResult.setFirstPlayerResult(result);
            } else
                throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "This player has already submitted his result.");

        } else if (player.getId() == matchResult.getSecondPlayer().getId()) {
            if (matchResult.getSecondPlayerResult() == null) {
                matchResult.setSecondPlayerResult(result);
            } else
                throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "This player has already submitted his result.");
        } else
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "This player is not allowed to add the result for this match!");

        if (bothResultsPresentAndEqual(matchResult)) {
            matchResult.setResult(result);
            resultService.applyResults(matchResult.getFirstPlayer().getId(), matchResult.getSecondPlayer().getId(), result, currentMatch.getTournament().getId());
            currentMatch.setState(GameState.ENDED);

            WebSocketManager.sendMessage(buildNotification("match_result_settled", matchResult));

        } else if (matchResult.getFirstPlayerResult() != null && matchResult.getSecondPlayerResult() != null) {

            WebSocketMessage conflictNotification = buildNotification("match_result_conflict", matchResult);

            WebSocketManager.sendMessageByUserKey(conflictNotification, matchResult.getFirstPlayer().getUserKey());
            WebSocketManager.sendMessageByUserKey(conflictNotification, matchResult.getSecondPlayer().getUserKey());

            if (currentMatch.getReferee() != null) {
                WebSocketManager.sendMessageByUserKey(conflictNotification, currentMatch.getReferee().getUserKey());
            }
        }
        currentMatch.setMatchResult(matchResult);

        return MatchMapper.entityToJSON(currentMatch);
    }

    private Result convertToResult(String resultString) {

        if (resultString.equals(Result.FIRST.getValue())) {
            return Result.FIRST;
        } else if (resultString.equals((Result.SECOND.getValue()))) {
            return Result.SECOND;
        } else if (resultString.equals(Result.DRAW.getValue())) {
            return Result.DRAW;
        } else
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid result!");
    }

    private boolean bothResultsPresentAndEqual(MatchResult matchResult) {
        if (matchResult.getFirstPlayerResult() != null && matchResult.getSecondPlayerResult() != null)
            if (matchResult.getFirstPlayerResult() == matchResult.getSecondPlayerResult())
                return true;
        return false;
    }

    @Transactional
    public MatchJSON reportMatchByReferee(String matchKey, String resultString) throws JsonProcessingException {
        Match currentMatch = matchRepository.findByMatchKey(matchKey)
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Match not found!"));
        if (currentMatch.getState() == GameState.ENDED) {
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "This match ended, cannot report a new result!");
        }

        MatchResult matchResult = currentMatch.getMatchResult();
        Result result = convertToResult(resultString);

        matchResult.setResult(result);
        currentMatch.setMatchResult(matchResult);
        resultService.applyResults(matchResult.getFirstPlayer().getId(), matchResult.getSecondPlayer().getId(), result, currentMatch.getTournament().getId());
        currentMatch.setState(GameState.ENDED);

        //Check if current round is over and switch to the next one
        Round currentRound = roundRepository.findByRoundKey(currentMatch.getRound().getRoundKey())
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Round not found!"));

        if (tournamentService.roundFinished(currentRound)
                && currentRound.getState() == GameState.ACTIVE) {
            tournamentService.switchToNextRound(currentRound.getTournament().getTournamentKey());
        }

        WebSocketManager.sendMessage(buildNotification("match_result_settled", matchResult));

        return MatchMapper.entityToJSON(currentMatch);
    }

    @Transactional
    public JsonResponse assignRefereeToMatch(String refereeKey, String matchKey) {

        Match match = matchRepository.findByMatchKey(matchKey).orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Tournament not found!"));
        User referee = userRepository.findByUserKey(refereeKey).orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "User not found!"));

        if (match.getReferee() == null) {
            match.setReferee(referee);
        } else {
            throw new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "Referee is already added to this match!");
        }

        return new JsonResponse().with("status", "ok")
                .with("message", "Referee added for this match!")
                .done();
    }

    public WebSocketMessage buildNotification(String type, MatchResult matchResult) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        Serializable notification;

        if (type.equals("match_result_settled")) {

            notification = ResultSettledNotification.builder()
                    .firstPlayerName(matchResult.getFirstPlayer().getFullName())
                    .secondPlayerName(matchResult.getSecondPlayer().getFullName())
                    .result(matchResult.getResult().getValue())
                    .build();
        } else {

            notification = ResultConflictNotification.builder()
                    .firstPlayerName(matchResult.getFirstPlayer().getFullName())
                    .firstPlayerResult(matchResult.getFirstPlayerResult() != null ? matchResult.getFirstPlayerResult().getValue() : null)
                    .secondPlayerName(matchResult.getSecondPlayer().getFullName())
                    .secondPlayerResult(matchResult.getSecondPlayerResult() != null ? matchResult.getSecondPlayerResult().getValue() : null)
                    .build();
        }

        return WebSocketMessage.builder()
                .type(type)
                .content(objectMapper.writeValueAsString(notification))
                .build();
    }

    public List<SearchMatchesJSON> searchMatches(MatchSearchCriteria matchSearchCriteria) {
        List<SearchMatchesJSON> matchList;

        if (matchSearchCriteria.isEmpty()) {
            matchList = matchRepository.findAll(where(hasNoGhostPlayer()))
                    .stream()
                    .sorted(Comparator.comparingInt(Match::getId))
                    .map(MatchMapper::entityToSearchMatchJSON)
                    .collect(Collectors.toList());
        } else {
            matchList = matchRepository.searchMatches(matchSearchCriteria);
        }

        if (!matchList.isEmpty()) {
            AtomicInteger previousRoundNumber = new AtomicInteger(matchList.get(0).getMatchNumber());
            AtomicInteger matchNumber = new AtomicInteger(0);
            AtomicReference<String> previousTournamentName = new AtomicReference<>(matchList.get(0).getTournamentName());

            matchList.forEach(match -> {
                if (previousRoundNumber.get() != match.getRoundNo() || !previousTournamentName.get().equals(match.getTournamentName())) {
                    previousTournamentName.set(match.getTournamentName());
                    previousRoundNumber.set(match.getRoundNo());
                    matchNumber.set(0);
                }

                match.setMatchNumber(matchNumber.incrementAndGet());
            });
        }

        return matchList;
    }

    private Specification<Match> hasNoGhostPlayer() {
        return (matchRoot, criteriaQuery, builder) -> {
            Join<Match, MatchResult> join = matchRoot.join("matchResult");

            return builder.and(
                    builder.notLike(join.get("firstPlayer").get("email"), "%" + "GHOST_EMAIL_" + "%"),
                    builder.notLike(join.get("secondPlayer").get("email"), "%" + "GHOST_EMAIL_" + "%")
            );
        };
    }

}