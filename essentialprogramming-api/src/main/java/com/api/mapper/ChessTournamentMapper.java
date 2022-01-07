package com.api.mapper;

import com.api.entities.Tournament;
import com.api.output.ChessTournamentJSON;
import lombok.SneakyThrows;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ChessTournamentMapper {

    @SneakyThrows
    public static ChessTournamentJSON entityToJSON(Tournament tournament)  {

        AtomicInteger index = new AtomicInteger(0);

        return ChessTournamentJSON.builder()
                .tournamentName(tournament.getName())
                .tournamentKey(tournament.getTournamentKey())
                .schedule(ScheduleMapper.scheduleToJSON(tournament.getSchedule()))
                .rounds(tournament.getRounds().values().stream()
                        .map(RoundMapper::entityToJSON)
                        .collect(Collectors.toMap(
                                c -> index.incrementAndGet(),
                                Function.identity())))
                .build();
    }
    public static String tournamentJSONToString(Tournament tournament){
        return entityToJSON(tournament).toString();
    }
}
