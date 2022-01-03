package com.api.mapper;

import com.api.entities.Tournament;
import com.api.env.resources.AppResources;
import com.api.model.GameState;
import com.api.model.TournamentInput;
import com.api.output.SearchTournamentsJSON;
import com.api.output.TournamentJSON;
import com.crypto.Crypt;
import lombok.SneakyThrows;

import java.util.stream.Collectors;

public class TournamentMapper {

    public static Tournament inputToTournament(TournamentInput tournamentInput) {

        return Tournament.builder()
                .name(tournamentInput.getName())
                .schedule(ScheduleMapper.inputToSchedule(tournamentInput.getSchedule()))
                .state(GameState.CREATED)
                .registrationOpen(tournamentInput.isRegistrationOpen())
                .maxParticipants(tournamentInput.getMaxParticipantsNo())
                .build();
    }

    @SneakyThrows
    public static TournamentJSON tournamentToJSON(Tournament tournament) {
        return TournamentJSON.builder()
                .schedule(ScheduleMapper.scheduleToJSON(tournament.getSchedule()))
                .tournamentKey(tournament.getTournamentKey())
                .name(tournament.getName())
                .registrationOpen(tournament.isRegistrationOpen())
                .maxParticipantsNo(tournament.getMaxParticipants())
                .referees(tournament.getReferees() == null ? null : tournament.getReferees().stream()
                        .map(UserMapper::userToJson).collect(Collectors.toList()))
                .build();
    }

    @SneakyThrows
    public static SearchTournamentsJSON tournamentToSearchTournamentsJSON(Tournament tournament){
        return SearchTournamentsJSON.builder()
                .name(tournament.getName())
                .encryptedTournamentKey(Crypt.encrypt(tournament.getTournamentKey(), AppResources.ENCRYPTION_KEY.value()))
                .maxParticipantsNo(tournament.getMaxParticipants())
                .registrationOpen(tournament.isRegistrationOpen())
                .build();
    }
}