package com.util;

import com.api.entities.*;
import com.api.model.*;
import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.api.entities.Schedule;
import com.api.entities.Tournament;
import com.api.entities.User;
import com.api.entities.UserPlatform;
import com.api.model.GameState;
import com.api.model.Result;
import com.api.model.ScheduleInput;
import com.api.model.TournamentInput;
import com.api.model.Type;
import com.api.model.UserInput;
import com.util.enums.PlatformType;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class TestEntityGenerator {

    private static final SecureRandom secureRandom = new SecureRandom();

    public static final DateTimeFormatter Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private TestEntityGenerator() {
    }

    public static User generateUser() {

        return User.builder().
                active(true)
                .id(1)
                .email("testEmail" + secureRandom.nextInt())
                .userKey("testUserKey" + secureRandom.nextInt())
                .firstName("testFirstName" + secureRandom.nextInt())
                .lastName("testLastName" + secureRandom.nextInt())
                .phone("testPhone" + secureRandom.nextInt())
                .userPlatformList(new ArrayList<>())
                .build();

    }

    public static Tournament generateTournament(GameState state, boolean isOpen, int maxParticipants) {
        return Tournament.builder()
                .schedule(TestEntityGenerator.generateSchedule())
                .state(state)
                .id(secureRandom.nextInt())
                .maxParticipants(maxParticipants)
                .name("Test Tournament " + secureRandom.nextInt())
                .tournamentKey("Test key " + secureRandom.nextInt())
                .registrationOpen(isOpen)
                .referees(new HashSet<>())
                .players(new ArrayList<>())
                .build();
    }

    public static Schedule generateSchedule() {
        LocalDateTime localDateTime = LocalDateTime.now();

        return Schedule.builder()
                .id(secureRandom.nextInt())
                .startDate(localDateTime)
                .endDate(localDateTime.plusMinutes(secureRandom.nextInt(500)))
                .location("Location " + secureRandom.nextInt())
                .build();
    }

    public static TournamentInput generateTournamentInput(boolean isOpen) {
        return TournamentInput.builder()
                .name("Test Tournament " + secureRandom.nextInt())
                .maxParticipantsNo(secureRandom.nextInt())
                .registrationOpen(isOpen)
                .build();
    }

    public static ScheduleInput generateScheduleInput() {
        LocalDateTime localDateTime = LocalDateTime.now();

        return ScheduleInput.builder()
                .location("location " + secureRandom.nextInt())
                .startDate(localDateTime.plusDays(1 + secureRandom.nextInt(5) ).format(Formatter))
                .endDate(localDateTime.plusDays(10 + secureRandom.nextInt(20)).format(Formatter))
                .build();

    }

    public static Player generatePlayer(Boolean deleted, Boolean active, Boolean validated) {
        return Player.builder()
                .id(secureRandom.nextInt())
                .email("email" + secureRandom.nextInt() + "@gmail.com")
                .password("password" + secureRandom.nextInt())
                .deleted(deleted)
                .active(active)
                .userKey("userkey" + secureRandom.nextInt())
                .phone("0712345678")
                .score(secureRandom.nextInt())
                .createdDate(LocalDateTime.now())
                .lastName("lastname")
                .firstName("firstname")
                .createdBy(secureRandom.nextInt())
                .modifiedDate(LocalDateTime.now())
                .modifiedBy(secureRandom.nextInt())
                .validated(validated)
                .playerTournaments(new ArrayList<>())
                .userSettings(new ArrayList<>())
                .build();
    }

    public static UserSettings generateUserSettings(boolean isActive, Type type) {
        return UserSettings.builder()
                .active(isActive)
                .id(secureRandom.nextInt())
                .createdDate(LocalDateTime.now())
                .expirationDate(LocalDateTime.now().plusMinutes(secureRandom.nextInt(500)))
                .type(type)
                .build();
    }

    public static Player generatePlayer(String email, String firstName, String lastName) {
        return Player.builder()
                .id(secureRandom.nextInt())
                .email(email)
                .score(secureRandom.nextInt())
                .firstName(firstName)
                .lastName(lastName)
                .phone(String.valueOf(secureRandom.nextInt()))
                .build();
    }

    public static List<Player> generatePlayerList(int playerCount) {

        List<Player> playerList = new ArrayList<>();

        for (int playerIndex = 0; playerIndex < playerCount; playerIndex++) {

            String playerEmail = String.format("player%d@gmail.com", playerIndex);
            String playerFirstName = String.format("player%d", playerIndex);
            String playerLastName = String.format("player%d", playerIndex);

            Player player = TestEntityGenerator.generatePlayer(playerEmail, playerFirstName, playerLastName);
            playerList.add(player);
        }
        return playerList;
    }

    public static Round generateRound(GameState state) {
        return Round.builder()
                .number(secureRandom.nextInt())
                .state(state)
                .roundKey("Test key " + NanoIdUtils.randomNanoId())
                .build();
    }

    public static UserInput generateUserInput() {
        return UserInput.builder()
                .email("Email " + secureRandom.nextInt())
                .firstName("First name " + secureRandom.nextInt())
                .lastName("Last name " + secureRandom.nextInt())
                .phone("Phone " + secureRandom.nextInt())
                .password("Password " + secureRandom.nextInt())
                .build();
    }

    public static TournamentUser generateTournamentUser(User user, Tournament tournament) {
        return new TournamentUser(tournament, user);

    }

    public static UserPlatform generateUserPlatform(PlatformType platformType) {
        return UserPlatform.builder()
                .platformType(platformType)
                .roles(Collections.singletonList(String.valueOf(platformType)))
                .build();
    }

    public static Match generateMatch(GameState state) {
        return Match.builder()
                .id(secureRandom.nextInt())
                .state(state)
                .startDate(LocalDateTime.now().plusMinutes(secureRandom.nextInt(500)))
                .matchKey("match_key_" + secureRandom.nextInt())
                .build();
    }

    public static MatchResult generateMatchResult(Result result) {
        return MatchResult.builder()
                .matchResultKey("Test key " + secureRandom.nextInt())
                .result(result)
                .build();
    }
}
