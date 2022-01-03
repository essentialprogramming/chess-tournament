package com.integration;

import com.api.entities.Match;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;

public class MatchTest {

    private static String accessToken;
    private static Match match;

    @BeforeAll
    public static void setup() {
        RestAssured.baseURI = "http://localhost:8080";
        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode userDetails = objectMapper.createObjectNode();
        userDetails.put("email", "string@AC.c");
        userDetails.put("password", "0123456789");
        userDetails.put("platform", "PLAYER");

        RestAssured.basePath = "/api/auth/token";
    }
}
