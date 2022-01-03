package com.integration;

import com.api.entities.Player;
import com.api.model.ScheduleInput;
import com.api.model.TournamentInput;
import com.api.model.UserInput;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.util.TestEntityGenerator;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class TournamentTest {

    private static final int NO_OF_TOURNAMENT_PLAYERS = 4;
    private static final int MAX_TOURNAMENT_PARTICIPANTS = 200;

    private static String superAdminAccessToken;
    private static String playerAccessToken;

    @BeforeAll
    public static void setup() {
        RestAssured.baseURI = "http://localhost:8080";

        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode userDetails = objectMapper.createObjectNode();
        userDetails.put("email","superadminavgchess@avangarde-software.com");
        userDetails.put("password", "iamsuper");
        userDetails.put("platform", "SUPER_ADMIN");

        RestAssured.basePath = "/api/auth/token";
        superAdminAccessToken = given()
                        .accept(ContentType.JSON)
                        .contentType(ContentType.JSON)
                        .header("Accept-Language", "en")
                        .body(userDetails)
                    .when()
                        .post()
                    .then()
                        .assertThat()
                        .statusCode(200)
                    .extract()
                    .path("accessToken");

        superAdminAccessToken = "Bearer " + superAdminAccessToken;

        System.out.println(superAdminAccessToken);
    }

    @Test
    public void create_get_start_end_delete_tournament() {
        TournamentInput tournamentInput = TestEntityGenerator.generateTournamentInput(true);
        tournamentInput.setMaxParticipantsNo(MAX_TOURNAMENT_PARTICIPANTS);

        ScheduleInput scheduleInput = TestEntityGenerator.generateScheduleInput();
        tournamentInput.setSchedule(scheduleInput);

        RestAssured.basePath = "/api";

        //createTournament
        String tournamentKey = given()
                                    .accept(ContentType.JSON)
                                    .contentType(ContentType.JSON)
                                    .header("Authorization", superAdminAccessToken)
                                    .body(tournamentInput)
                               .when()
                                    .post("/tournament/create")
                                .then()
                                    .assertThat()
                                    .statusCode(201)
                                .and()
                                    .contentType(ContentType.JSON)
                                .and()
                                    .body("name", equalTo(tournamentInput.getName()))
                                .and()
                                    .body("registrationOpen", equalTo(tournamentInput.isRegistrationOpen()))
                                .and()
                                    .body("maxParticipantsNo", equalTo(tournamentInput.getMaxParticipantsNo()))
                                    .extract()
                                    .path("tournamentKey");

        //getTournament
        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", superAdminAccessToken)
                .queryParam("tournamentKey", tournamentKey)
        .when()
                .get("/tournament")
        .then()
                .assertThat()
                .statusCode(200)
        .and()
                .contentType(ContentType.JSON)
        .and()
                .body("tournamentName", equalTo(tournamentInput.getName()))
        .and()
                .body("tournamentKey", equalTo(tournamentKey))
        .and()
                .body("schedule.startDate", equalTo(scheduleInput.getStartDate()))
        .and()
                .body("schedule.endDate", equalTo(scheduleInput.getEndDate()))
        .and()
                .body("schedule.location", equalTo(scheduleInput.getLocation()));

        //create players (required for startTournament)
        List<String> playerKeys = new ArrayList<>();

        Player randomPlayer = new Player();

        for (int noOfPlayers = 0; noOfPlayers < NO_OF_TOURNAMENT_PLAYERS; noOfPlayers++) {
            Player player = TestEntityGenerator.generatePlayer(false, true, false);

            UserInput playerInput = UserInput.builder()
                            .email(player.getEmail())
                            .firstName(player.getFirstName())
                            .lastName(player.getLastName())
                            .phone(player.getPhone())
                            .password(player.getPassword())
                            .confirmPassword(player.getPassword())
                            .build();

            String playerKey = given()
                                    .accept(ContentType.JSON)
                                    .contentType(ContentType.JSON)
                                    .header("Authorization", superAdminAccessToken)
                                    .header("Accept-Language", "en")
                                    .body(playerInput)
                               .when()
                                    .post("/security/player/create")
                               .then()
                                    .assertThat()
                                    .statusCode(201)
                               .and()
                                    .body("email", equalTo(player.getEmail()))
                               .and()
                                    .body("firstName", equalTo(player.getFirstName()))
                               .and()
                                    .body("lastName", equalTo(player.getLastName()))
                               .and()
                                    .body("score", equalTo(0F))
                                    .extract()
                                    .path("playerKey");

            playerKeys.add(playerKey);

            if (noOfPlayers == NO_OF_TOURNAMENT_PLAYERS - 1) {
                randomPlayer = player;
            }
        }

        //invite players (required for startTournament)
        playerKeys.forEach(key -> given()
                                    .accept(ContentType.JSON)
                                    .contentType(ContentType.JSON)
                                    .header("Authorization", superAdminAccessToken)
                                    .queryParam("user_key", key)
                                    .queryParam("tournament_key", tournamentKey)
                                .when()
                                    .post("/tournament/invite")
                                .then()
                                    .assertThat()
                                    .statusCode(200)
                                .and()
                                    .contentType(ContentType.JSON)
                                .and()
                                    .body("status", equalTo("ok"))
                                .and()
                                    .body("message", equalTo("Player invited to the tournament!")));

        //confirm participation in tournament (required for startTournament)
        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode userDetails = objectMapper.createObjectNode();
        userDetails.put("email", randomPlayer.getEmail());
        userDetails.put("password", randomPlayer.getPassword());
        userDetails.put("platform", "PLAYER");

        playerAccessToken = given()
                                .accept(ContentType.JSON)
                                .contentType(ContentType.JSON)
                                .header("Accept-Language", "en")
                                .body(userDetails)
                            .when()
                                .post("/auth/token")
                            .then()
                                .assertThat()
                                .statusCode(200)
                                .extract()
                                .path("accessToken");

        playerAccessToken = "Bearer " + playerAccessToken;

        playerKeys.forEach(key -> given()
                                    .accept(ContentType.JSON)
                                    .contentType(ContentType.JSON)
                                    .header("Authorization", playerAccessToken)
                                    .pathParam("user_key", key)
                                    .pathParam("tournament_key", tournamentKey)
                                .when()
                                    .get("/tournament/confirmation/{user_key}/{tournament_key}")
                                .then()
                                    .assertThat()
                                    .statusCode(200)
                                .and()
                                    .body("status", equalTo("ok"))
                                .and()
                                    .body("userKey", equalTo(key)));

        //startTournament
        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", superAdminAccessToken)
                .queryParam("tournament_key", tournamentKey)
        .when()
                .post("/tournament/start")
        .then()
                .assertThat()
                .statusCode(200);

        //endTournament
        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", superAdminAccessToken)
                .queryParam("tournament_key", tournamentKey)
        .when()
                .post("/tournament/end")
        .then()
                .assertThat()
                .statusCode(200)
        .and()
                .contentType(ContentType.JSON)
        .and()
                .body("status", equalTo("ok"))
        .and()
                .body("message", equalTo("Tournament ended."));

        //deleteTournament
        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", superAdminAccessToken)
                .queryParam("tournament_key", tournamentKey)
        .when()
                .delete("/tournament")
        .then()
                .assertThat()
                .statusCode(200)
        .and()
                .contentType(ContentType.JSON)
        .and()
                .body("status", equalTo("ok"))
        .and()
                .body("message", equalTo("Tournament and associated relations has been deleted."));
    }

    @AfterAll
    public static void afterAll() {
        RestAssured.reset();
    }
}


