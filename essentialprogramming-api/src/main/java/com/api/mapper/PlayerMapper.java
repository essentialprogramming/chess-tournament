package com.api.mapper;

import com.api.entities.Player;

import com.api.entities.User;
import com.api.model.UserInput;
import com.api.output.PlayerJSON;
import com.api.output.SearchParticipantsJSON;
import com.api.output.SearchRefereeJSON;

public class PlayerMapper {

    public static PlayerJSON playerToJson(Player player) {
        return PlayerJSON.builder()
                .email(player.getEmail())
                .firstName(player.getFirstName())
                .lastName(player.getLastName())
                .score(player.getScore())
                .playerKey(player.getUserKey())
                .build();
    }


    public static Player inputToPlayer(UserInput input) {
        return Player.builder()
                .firstName(input.getFirstName())
                .lastName(input.getLastName())
                .email(input.getEmail())
                .phone(input.getPhone())
                .build();
    }

    public static PlayerJSON userToPlayerJson(User user) {
        return PlayerJSON.builder()
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .playerKey(user.getUserKey())
                .build();
    }

    public static SearchParticipantsJSON playerToSearchParticipantsJSON(User user) {
        return SearchParticipantsJSON.builder()
                .playerKey(user.getUserKey())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

    public static SearchRefereeJSON refereeToSearchRefereeJSON(User user, long numberOfAssignedMatches) {
        return SearchRefereeJSON.builder()
                .userKey(user.getUserKey())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .numberOfAssignedMatches(numberOfAssignedMatches)
                .build();
    }
}