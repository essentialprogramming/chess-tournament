package com.api.repository;

import com.api.model.ParticipantSearchCriteria;
import com.api.output.SearchParticipantsJSON;

import java.util.List;

public interface PlayerRepositoryCustom {
    List<SearchParticipantsJSON> searchPlayers(ParticipantSearchCriteria participantSearchCriteria);
}
