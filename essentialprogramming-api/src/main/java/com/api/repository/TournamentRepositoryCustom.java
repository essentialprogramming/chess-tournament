package com.api.repository;

import com.api.model.TournamentSearchCriteria;
import com.api.output.SearchTournamentsJSON;
import java.util.List;

public interface TournamentRepositoryCustom {
    List<SearchTournamentsJSON> searchTournaments(TournamentSearchCriteria tournamentSearchCriteria);
}
