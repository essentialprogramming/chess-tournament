package com.api.repository.impl;

import com.api.entities.Match;
import com.api.entities.MatchResult;
import com.api.entities.Player;
import com.api.entities.User;
import com.api.model.GameState;
import com.api.model.MatchSearchCriteria;
import com.api.model.Result;
import com.api.output.SearchMatchesJSON;
import com.api.repository.MatchRepositoryCustom;
import com.util.date.DateUtil;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MatchRepositoryCustomImpl implements MatchRepositoryCustom {
    private final LocalDateTime START_DATE_SENTINEL = LocalDateTime.of(1970, 1, 1, 0, 0);
    private final LocalDateTime END_DATE_SENTINEL = LocalDateTime.now().plusYears(3000);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<SearchMatchesJSON> searchMatches(MatchSearchCriteria matchSearchCriteria) {
        final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<SearchMatchesJSON> criteriaQuery = criteriaBuilder.createQuery(SearchMatchesJSON.class);
        final Root<Match> matchRoot = criteriaQuery.from(Match.class);

        Predicate firstWon = criteriaBuilder.exists(getFirstIfWon(criteriaBuilder, criteriaQuery, matchRoot));
        Predicate secondWon = criteriaBuilder.exists(getSecondIfWon(criteriaBuilder, criteriaQuery, matchRoot));
        Predicate draw = criteriaBuilder.exists(getDraw(criteriaBuilder, criteriaQuery, matchRoot));

        Predicate isActive = criteriaBuilder.equal(matchRoot.get("state"), GameState.ACTIVE);
        Predicate isEnded = criteriaBuilder.equal(matchRoot.get("state"), GameState.ENDED);
        Predicate isCreated = criteriaBuilder.equal(matchRoot.get("state"), GameState.CREATED);
        Predicate unsettledResult = criteriaBuilder.or(isActive, isCreated);


        Join<Match, User> join = matchRoot.join("referee", JoinType.LEFT);
        Predicate refereeExists = criteriaBuilder.isNotNull(join);

        criteriaQuery.select(criteriaBuilder.construct(SearchMatchesJSON.class,
                criteriaBuilder.selectCase()
                        .when(isActive, "ACTIVE")
                        .when(isEnded, "ENDED")
                        .when(isCreated, "CREATED"),
                matchRoot.get("round").get("number"),
                matchRoot.get("tournament").get("name"),
                criteriaBuilder.selectCase()
                        .when(refereeExists, matchRoot.get("referee").get("email"))
                        .otherwise("No referee assigned"),
                criteriaBuilder.selectCase()
                        .when(firstWon, "FIRST")
                        .when(secondWon, "SECOND")
                        .when(draw, "DRAW")
                        .otherwise("No result"),
                criteriaBuilder.selectCase()
                        .when(unsettledResult, "Match result not settled yet")
                        .when(firstWon, getFirstIfWon(criteriaBuilder, criteriaQuery, matchRoot).getSelection())
                        .when(secondWon, getSecondIfWon(criteriaBuilder, criteriaQuery, matchRoot).getSelection())
                        .when(draw, "DRAW")
                        .otherwise("No result")
                ))
                .orderBy(criteriaBuilder.asc(matchRoot.get("id"))); //SELECT FROM

        List<Predicate> predicates = getFilterPredicates(matchSearchCriteria, criteriaBuilder, matchRoot, criteriaQuery);
        if (!predicates.isEmpty()) {
            criteriaQuery.where(predicates.toArray(new Predicate[]{})); //WHERE
        }

        return entityManager.createQuery(criteriaQuery).getResultList();
    }

    private Subquery<Object> getFirstIfWon(CriteriaBuilder builder, CriteriaQuery<?> criteriaQuery, Root<Match> matchRoot) {

        Subquery<Object> subquery = criteriaQuery.subquery(Object.class);
        Root<MatchResult> matchResultRoot = subquery.from(MatchResult.class);

        Predicate idsEqual = builder.equal(matchResultRoot.get("id"), matchRoot.get("matchResult").get("id"));
        Predicate firstPlayerWin = builder.equal(getResult(builder, criteriaQuery, matchRoot).getSelection(), Result.FIRST);
        Predicate conditions = builder.and(idsEqual, firstPlayerWin);

        subquery.select(matchResultRoot.get("firstPlayer").get("email")).where(conditions);
        return subquery;
    }

    private Subquery<Object> getSecondIfWon(CriteriaBuilder builder, CriteriaQuery<?> criteriaQuery, Root<Match> matchRoot) {
        Subquery<Object> subquery = criteriaQuery.subquery(Object.class);
        Root<MatchResult> matchResultRoot = subquery.from(MatchResult.class);

        Predicate idsEqual = builder.equal(matchResultRoot.get("id"), matchRoot.get("matchResult").get("id"));
        Predicate secondPlayerWin = builder.equal(getResult(builder, criteriaQuery, matchRoot).getSelection(), Result.SECOND);
        Predicate conditions = builder.and(idsEqual, secondPlayerWin);

        subquery.select(matchResultRoot.get("secondPlayer").get("email")).where(conditions);
        return subquery;
    }

    private Subquery<Object> getDraw(CriteriaBuilder builder, CriteriaQuery<?> criteriaQuery, Root<Match> matchRoot) {
        Subquery<Object> subquery = criteriaQuery.subquery(Object.class);
        Root<MatchResult> matchResultRoot = subquery.from(MatchResult.class);

        Predicate idsEqual = builder.equal(matchResultRoot.get("id"), matchRoot.get("matchResult").get("id"));
        Predicate draw = builder.equal(getResult(builder, criteriaQuery, matchRoot).getSelection(), Result.DRAW);
        Predicate condition = builder.and(idsEqual, draw);

        subquery.select(matchResultRoot.get("result")).where(condition);
        return subquery;
    }

    private Subquery<Result> getResult(CriteriaBuilder builder, CriteriaQuery<?> criteriaQuery, Root<Match> matchRoot) {
        Subquery<Result> subquery = criteriaQuery.subquery(Result.class);
        Root<MatchResult> matchResultRoot = subquery.from(MatchResult.class);

        Predicate condition = builder.equal(matchResultRoot.get("id"), matchRoot.get("matchResult").get("id"));

        subquery.select(matchResultRoot.get("result")).where(condition);
        return subquery;
    }

    private Subquery<String> getFirstPlayerEmail(CriteriaBuilder builder, CriteriaQuery<?> criteriaQuery, Root<Match> matchRoot) {
        Subquery<String> subquery = criteriaQuery.subquery(String.class);
        Root<MatchResult> matchResultRoot = subquery.from(MatchResult.class);


        Predicate idsEqual = builder.equal(matchResultRoot.get("id"), matchRoot.get("matchResult").get("id"));
        return subquery.select(matchResultRoot.get("firstPlayer").get("email")).where(idsEqual);
    }

    private Subquery<String> getSecondPlayerEmail(CriteriaBuilder builder, CriteriaQuery<?> criteriaQuery, Root<Match> matchRoot) {
        Subquery<String> subquery = criteriaQuery.subquery(String.class);
        Root<MatchResult> matchResultRoot = subquery.from(MatchResult.class);


        Predicate idsEqual = builder.equal(matchResultRoot.get("id"), matchRoot.get("matchResult").get("id"));
        return subquery.select(matchResultRoot.get("secondPlayer").get("email")).where(idsEqual);
    }

    private List<Predicate> getFilterPredicates(MatchSearchCriteria matchSearchCriteria, CriteriaBuilder builder, Root<Match> matchRoot, CriteriaQuery<?> criteriaQuery) {
        Predicate stateCondition = null;
        Predicate roundNoCondition = null;
        Predicate tournamentNameCondition = null;
        Predicate refereeCondition = null;
        Predicate matchResultCondition = null;
        Predicate startDateCondition;
        Predicate firstPlayerNotGhost = builder.notLike(getFirstPlayerEmail(builder, criteriaQuery, matchRoot), "%" + "GHOST_EMAIL_" + "%");
        Predicate secondPlayerNotGhost = builder.notLike(getSecondPlayerEmail(builder, criteriaQuery, matchRoot), "%" + "GHOST_EMAIL_" + "%");
        LocalDateTime startDateMin;
        LocalDateTime startDateMax;

        //Search by state
        if (matchSearchCriteria.getMatchState() != null) {
            switch (matchSearchCriteria.getMatchState()) {
                case "ACTIVE":
                    stateCondition = builder.equal(matchRoot.get("state"), GameState.ACTIVE);
                    break;
                case "ENDED":
                    stateCondition = builder.equal(matchRoot.get("state"), GameState.ENDED);
                    break;
                case "CREATED":
                    stateCondition = builder.equal(matchRoot.get("state"), GameState.CREATED);
                    break;
            }
        }

        //Search by result
        if(matchSearchCriteria.getResult() != null) {
            switch(matchSearchCriteria.getResult()) {
                case "FIRST":
                    matchResultCondition = builder.equal(getResult(builder, criteriaQuery, matchRoot), Result.FIRST);
                    break;
                case "SECOND":
                    matchResultCondition = builder.equal(getResult(builder, criteriaQuery, matchRoot), Result.SECOND);
                    break;
                case "DRAW":
                    matchResultCondition = builder.equal(getResult(builder, criteriaQuery, matchRoot), Result.DRAW);
                    break;
            }
        }

        //Search by round number
        if (matchSearchCriteria.getRoundNo() != null) {
            roundNoCondition = builder.equal(matchRoot.get("round").get("number"), matchSearchCriteria.getRoundNo());
        }

        //Search by tournament name
        if (matchSearchCriteria.getTournamentName() != null) {
            tournamentNameCondition = builder.like(builder.lower(matchRoot.get("tournament").get("name")),
                    "%" + matchSearchCriteria.getTournamentName().toLowerCase() + "%");
        }

        //Search by referee email, firstname or lastname
        if (matchSearchCriteria.getReferee() != null) {
            Predicate refereeEmailCondition = builder.like(builder.lower(matchRoot.get("referee").get("email")),
                    "%" + matchSearchCriteria.getReferee().toLowerCase() + "%");
            Predicate refereeFirstnameCondition = builder.like(builder.lower(matchRoot.get("referee").get("firstName")),
                    "%" + matchSearchCriteria.getReferee().toLowerCase() + "%");
            Predicate refereeLastnameCondition = builder.like(builder.lower(matchRoot.get("referee").get("lastName")),
                    "%" + matchSearchCriteria.getReferee().toLowerCase() + "%");

            refereeCondition = builder.or(refereeEmailCondition, refereeFirstnameCondition, refereeLastnameCondition);
        }

        //Search by start date
        startDateMin = matchSearchCriteria.getStartDateMin() != null ? DateUtil.stringToDate(matchSearchCriteria.getStartDateMin()) : START_DATE_SENTINEL;
        startDateMax = matchSearchCriteria.getStartDateMax() != null ? DateUtil.stringToDate(matchSearchCriteria.getStartDateMax()) : END_DATE_SENTINEL;

        startDateCondition = builder.between(
                matchRoot.get("startDate"),
                startDateMin,
                startDateMax
        );

        return Stream.of(stateCondition,
                        roundNoCondition,
                        tournamentNameCondition,
                        refereeCondition,
                        startDateCondition,
                        matchResultCondition,
                        firstPlayerNotGhost,
                        secondPlayerNotGhost)

                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
