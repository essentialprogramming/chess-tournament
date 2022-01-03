package com.api.repository;

import com.api.model.MatchSearchCriteria;
import com.api.output.SearchMatchesJSON;

import java.util.List;

public interface MatchRepositoryCustom {
    List<SearchMatchesJSON> searchMatches(MatchSearchCriteria matchSearchCriteria);
}
