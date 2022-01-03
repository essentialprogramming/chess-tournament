package com.api.service;

import com.api.entities.Player;
import com.api.entities.Tournament;
import com.api.entities.TournamentUser;
import com.api.entities.TournamentUserKey;
import com.api.model.Result;
import com.api.repository.PlayerRepository;
import com.api.repository.TournamentRepository;
import com.api.repository.TournamentUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ResultService {

    private final PlayerRepository playerRepository;
    private final TournamentUserRepository tournamentUserRepository;

    public void applyResults(int firstPlayerId, int secondPlayerId, Result result, int tournamentId) {

        Player firstPlayer = playerRepository.findById(firstPlayerId).orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Player not found!"));
        Player secondPlayer = playerRepository.findById(secondPlayerId).orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Player not found!"));

        TournamentUser tournamentUserFirstPlayer = tournamentUserRepository.findById(new TournamentUserKey(tournamentId, firstPlayerId))
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "This player is not participating in the tournament"));
        TournamentUser tournamentUserSecondPlayer = tournamentUserRepository.findById(new TournamentUserKey(tournamentId, secondPlayerId))
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "This player is not participating in the tournament"));

        switch (result) {
            case FIRST:
                firstPlayer.setScore(firstPlayer.getScore() + 1);
                tournamentUserFirstPlayer.setScore(tournamentUserFirstPlayer.getScore() + 1);
                break;
            case SECOND:
                secondPlayer.setScore(secondPlayer.getScore() + 1);
                tournamentUserSecondPlayer.setScore(tournamentUserSecondPlayer.getScore() + 1);
                break;
            case DRAW:
                firstPlayer.setScore(firstPlayer.getScore() + 0.5);
                secondPlayer.setScore(secondPlayer.getScore() + 0.5);
                tournamentUserFirstPlayer.setScore(tournamentUserFirstPlayer.getScore() + 0.5);
                tournamentUserSecondPlayer.setScore(tournamentUserSecondPlayer.getScore() + 0.5);
        }
    }

    public void applyResultForGhost(int playerId, int tournamentId) {

        Player player = playerRepository.findById(playerId).orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "Player not found!"));
        player.setScore(player.getScore() + 1);

        TournamentUser tournamentPlayer = tournamentUserRepository.findById(new TournamentUserKey(tournamentId, playerId))
                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "This player is not participating in the tournament"));
        tournamentPlayer.setScore(tournamentPlayer.getScore() + 1);
    }
}
