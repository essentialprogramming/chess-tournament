package com.api.mapper;

import com.api.entities.User;
import com.api.model.InvitationStatus;
import com.api.output.ParticipantStatusJSON;

public class ParticipantStatusMapper {

    public static ParticipantStatusJSON createJSON(User user, InvitationStatus status) {
        return ParticipantStatusJSON.builder()
                .userJSON(UserMapper.userToJson(user))
                .status(status.toString())
                .build();
    }
}
