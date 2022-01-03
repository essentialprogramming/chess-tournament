package com.api.repository;

import com.api.model.RefereeSearchCriteria;
import com.api.output.SearchRefereeJSON;

import java.util.List;

public interface RefereeRepositoryCustom {
    List<SearchRefereeJSON> searchReferee(RefereeSearchCriteria refereeSearchCriteria);
}
