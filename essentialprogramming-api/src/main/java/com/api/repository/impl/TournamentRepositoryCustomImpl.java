package com.api.repository.impl;

import com.api.entities.*;
import com.api.entities.TournamentUser;
import com.api.entities.User;
import com.api.model.GameState;
import com.api.model.TournamentSearchCriteria;
import com.api.output.SearchTournamentsJSON;
import com.api.repository.TournamentRepositoryCustom;
import com.util.date.DateUtil;


import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TournamentRepositoryCustomImpl implements TournamentRepositoryCustom {
    private final LocalDateTime MIN_DATE_SENTINEL = LocalDateTime.of(1970, 1, 1, 0, 0);
    private final LocalDateTime MAX_DATE_SENTINEL = LocalDateTime.now().plusYears(3000);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<SearchTournamentsJSON> searchTournaments(TournamentSearchCriteria tournamentSearchCriteria) {
        final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<SearchTournamentsJSON> criteriaQuery = criteriaBuilder.createQuery(SearchTournamentsJSON.class);
        final Root<Tournament> tournamentRoot = criteriaQuery.from(Tournament.class);

        Predicate tournamentEndedCondition = criteriaBuilder.equal(tournamentRoot.get("state"), GameState.ENDED);
        Predicate multipleWinnersCondition = criteriaBuilder.notEqual(countWinnersSubquery(criteriaBuilder, criteriaQuery, tournamentRoot), 1);
        Predicate multipleWinnersConditions = criteriaBuilder.and(multipleWinnersCondition, tournamentEndedCondition);
        Predicate hasWinner = criteriaBuilder.exists(generateWinnerSubquery(criteriaBuilder,criteriaQuery,tournamentRoot));
        Subquery<String> winnerSubquery = generateWinnerSubquery(criteriaBuilder, criteriaQuery, tournamentRoot);

        criteriaQuery.select(criteriaBuilder.construct(
                SearchTournamentsJSON.class,
                tournamentRoot.get("name"),
                tournamentRoot.get("tournamentKey"),
                tournamentRoot.get("maxParticipants"),
                tournamentRoot.get("registrationOpen"),
                criteriaBuilder.selectCase()
                        .when(hasWinner, winnerSubquery.getSelection())
                        .when(multipleWinnersConditions, "MULTIPLE WINNERS")
                        .otherwise("NO WINNER"))
        );//SELECT FROM

        List<Predicate> predicates = getFilterPredicates(tournamentSearchCriteria, criteriaBuilder, tournamentRoot);
        if (!predicates.isEmpty()) {
            criteriaQuery.where(predicates.toArray(new Predicate[]{})); //WHERE
        }

        return entityManager.createQuery(criteriaQuery).getResultList();
    }

    private Subquery<String> generateWinnerSubquery(CriteriaBuilder builder, CriteriaQuery<?> criteriaQuery, Root<Tournament> tournamentRoot) {
        Subquery<String> subquery = criteriaQuery.subquery(String.class);
        Root<TournamentUser> tournamentUserRoot = subquery.from(TournamentUser.class);

        Subquery<Double> scoreSubquery = generateWinningScoreSubquery(builder, criteriaQuery, tournamentRoot);

        Predicate oneWinnerCondition = builder.equal(countWinnersSubquery(builder, criteriaQuery, tournamentRoot), 1);
        Predicate tournamentEndedCondition = builder.equal(tournamentRoot.get("state"), GameState.ENDED);
        Predicate tournamentCondition = builder.equal(tournamentUserRoot.get("tournament").get("id"), tournamentRoot.get("id"));
        Predicate scoreCondition = builder.equal(tournamentUserRoot.get("score"), scoreSubquery.getSelection());
        Predicate condition = builder.and(tournamentEndedCondition,tournamentCondition, scoreCondition,oneWinnerCondition);

        subquery.select(tournamentUserRoot.get("user").get("email")).where(condition);

        return subquery;
    }

    private Subquery<Long> countWinnersSubquery (CriteriaBuilder builder, CriteriaQuery<?> criteriaQuery,
                                                 Root<Tournament> tournamentRoot) {

        Subquery<Long> subquery = criteriaQuery.subquery(Long.class);
        Root<TournamentUser> tournamentUserRoot = subquery.from(TournamentUser.class);

        Subquery<Double> scoreSubquery = generateWinningScoreSubquery(builder, criteriaQuery, tournamentRoot);
        Predicate scoreCondition = builder.equal(tournamentUserRoot.get("score"), scoreSubquery.getSelection());

        subquery.select(builder.count(tournamentUserRoot)).where(scoreCondition);
        return subquery;
    }

    private Subquery<Double> generateWinningScoreSubquery(CriteriaBuilder builder, CriteriaQuery<?> criteriaQuery,
                                                          Root<Tournament> tournamentRoot) {

        Subquery<Double> subquery = criteriaQuery.subquery(Double.class);
        Root<TournamentUser> tournamentUserRoot = subquery.from(TournamentUser.class);

//        Predicate tournamentCondition = builder.equal(tournamentUserRoot.get("tournament").get("id"), tournamentRoot.get("id"));
        Join<TournamentUser, Tournament> tournamentJoin = tournamentUserRoot.join("tournament", JoinType.INNER);
        tournamentJoin.on(builder.equal(tournamentUserRoot.get("tournament").get("id"), tournamentRoot.get("id")));

        subquery.select(builder.max(tournamentUserRoot.get("score")));
        return subquery;
    }


    private List<Predicate> getFilterPredicates(TournamentSearchCriteria tournamentSearchCriteria, CriteriaBuilder builder, Root<Tournament> tournamentRoot) {
        Predicate nameCondition = null;
        Predicate dateCondition= null;
        LocalDateTime createdDateStart;
        LocalDateTime createdDateEnd;
        Join<Tournament, Schedule> schedule = tournamentRoot.join("schedule", JoinType.INNER);

        createdDateStart = tournamentSearchCriteria.getStartDate() != null ? DateUtil.stringToDate(tournamentSearchCriteria.getStartDate()) : MIN_DATE_SENTINEL;
        createdDateEnd = tournamentSearchCriteria.getEndDate() != null ? DateUtil.stringToDate(tournamentSearchCriteria.getEndDate()) : MAX_DATE_SENTINEL;

        //Search tournaments by date
        if (tournamentSearchCriteria.getStartDate() != null && tournamentSearchCriteria.getEndDate() == null) {
            dateCondition = builder.between(
                    schedule.get("startDate"),
                    createdDateStart,
                    createdDateEnd);
        }
        else if(tournamentSearchCriteria.getStartDate() == null && tournamentSearchCriteria.getEndDate() != null){
            dateCondition = builder.between(
                    schedule.get("endDate"),
                    createdDateStart,
                    createdDateEnd);
        }
        else if (tournamentSearchCriteria.getStartDate() != null && tournamentSearchCriteria.getEndDate() != null){
            Predicate startDateCondition= builder.greaterThanOrEqualTo(schedule.get("startDate"), createdDateStart);
            Predicate endDateCondition =  builder.lessThanOrEqualTo(schedule.get("endDate"), createdDateEnd);
            dateCondition = builder.and(startDateCondition, endDateCondition);
        }

        //Search tournaments by name
        if(tournamentSearchCriteria.getName() != null) {
            nameCondition =  builder.equal(tournamentRoot.get("name"), tournamentSearchCriteria.getName());
        }

        return Stream.of(nameCondition, dateCondition)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}