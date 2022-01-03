package com.api.repository.impl;

import com.api.entities.*;
import com.api.model.InvitationStatus;
import com.api.model.ParticipantSearchCriteria;
import com.api.output.SearchParticipantsJSON;
import com.api.repository.PlayerRepositoryCustom;
import com.util.date.DateUtil;
import com.util.text.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PlayerRepositoryCustomImpl implements PlayerRepositoryCustom {
    private final LocalDateTime START_DATE_SENTINEL = LocalDateTime.of(1970, 1, 1, 0, 0);
    private final LocalDateTime END_DATE_SENTINEL = LocalDateTime.now().plusYears(3000);

    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public List<SearchParticipantsJSON> searchPlayers(ParticipantSearchCriteria participantSearchCriteria) {

        final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<SearchParticipantsJSON> criteriaQuery = criteriaBuilder.createQuery(SearchParticipantsJSON.class);
        final Root<Player> playerRoot = criteriaQuery.from(Player.class);
        Predicate isInTournament = criteriaBuilder.exists(generateSubqueryAcceptedParticipants(criteriaBuilder, criteriaQuery, playerRoot));
        Predicate isPlayerInvitationInPending = criteriaBuilder.exists(generateSubqueryPendingParticipants(criteriaBuilder,criteriaQuery,playerRoot));

        Subquery<Long> totalMatchesSubquery = generateSubqueryForTotalMatches(criteriaBuilder, criteriaQuery, playerRoot);

        criteriaQuery.select(criteriaBuilder.construct(
                SearchParticipantsJSON.class,
                playerRoot.get("userKey"),
                playerRoot.get("email"),
                playerRoot.get("firstName"),
                playerRoot.get("lastName"),
                playerRoot.get("score"),
                totalMatchesSubquery.getSelection(),
                criteriaBuilder.selectCase()
                        .when(isInTournament,"ACCEPTED")
                        .when(isPlayerInvitationInPending,"PENDING")
                        .otherwise("REJECTED")
                ))
                .groupBy(playerRoot.get("firstName"), playerRoot.get("lastName"), playerRoot.get("email"), playerRoot.get("id"), playerRoot.get("score")); //SELECT FROM

        List<Predicate> predicates = getFilterPredicates(participantSearchCriteria, criteriaBuilder, playerRoot);
        if (!predicates.isEmpty()) {
            criteriaQuery.where(predicates.toArray(new Predicate[]{})); //WHERE
        }
        criteriaQuery.orderBy(getOrder(participantSearchCriteria, criteriaBuilder, playerRoot)); //ORDER BY

        return entityManager.createQuery(criteriaQuery).getResultList();
    }

    private Order getOrder(ParticipantSearchCriteria participantSearchCriteria, CriteriaBuilder builder, Root<Player> playerRoot){
        if (StringUtils.isNotEmpty(participantSearchCriteria.getSortKey())) {
            Expression<?> expression = getOrderColumn(participantSearchCriteria, builder, playerRoot);
            if ("desc".equalsIgnoreCase(participantSearchCriteria.getSortOrder()))
                return builder.desc(expression);
            else if ("asc".equalsIgnoreCase(participantSearchCriteria.getSortOrder()))
                return builder.asc(expression);
        }
        return builder.asc(playerRoot.get("firstName"));
    }

    private Expression<?> getOrderColumn(ParticipantSearchCriteria participantSearchCriteria, CriteriaBuilder builder, Root<Player> playerRoot){
        if (participantSearchCriteria.getSortKey().equals("score")) {
            return playerRoot.get(participantSearchCriteria.getSortKey());
        }
            return builder.trim(builder.lower(playerRoot.get(participantSearchCriteria.getSortKey())));
    }

    private Subquery<Long> generateSubqueryForTotalMatches(CriteriaBuilder criteriaBuilder, CriteriaQuery<?> criteriaQuery,
                                                               Root<Player> playerRoot) {

        Subquery<Long> subquery = criteriaQuery.subquery(Long.class);
        Root<MatchPlayer> matchPlayerRoot = subquery.from(MatchPlayer.class);

        Predicate firstPlayerPredicate = criteriaBuilder.equal(matchPlayerRoot.get("firstPlayer").get("email"), playerRoot.get("email"));
        Predicate secondPlayerPredicate = criteriaBuilder.equal(matchPlayerRoot.get("secondPlayer").get("email"), playerRoot.get("email"));
        Predicate condition = criteriaBuilder.or(firstPlayerPredicate, secondPlayerPredicate);

        subquery.select(criteriaBuilder.count(matchPlayerRoot)).where(condition);

        return subquery;
    }

    private Subquery<String> generateSubqueryAcceptedParticipants(CriteriaBuilder criteriaBuilder, CriteriaQuery<?> criteriaQuery, Root<Player> playerRoot) {
        Subquery<String> userSubquery = criteriaQuery.subquery(String.class);
        Root<TournamentUser> tournamentUserRoot = userSubquery.from(TournamentUser.class);

        Join<TournamentUser, User> user = tournamentUserRoot.join("user", JoinType.INNER);
        user.on(criteriaBuilder.equal(tournamentUserRoot.get("user").get("id"), playerRoot.get("id")));

        userSubquery.select(tournamentUserRoot.get("user").get("email"));

        return userSubquery;
    }

    private Subquery<String> generateSubqueryPendingParticipants(CriteriaBuilder criteriaBuilder, CriteriaQuery<?> criteriaQuery, Root<Player> playerRoot) {
        Subquery<String> userSubquery = criteriaQuery.subquery(String.class);
        Root<UserSettings> userSettingsRoot = userSubquery.from(UserSettings.class);

        Join<UserSettings, User> userJoin = userSettingsRoot.join("player", JoinType.INNER);
        userJoin.on(criteriaBuilder.equal(userSettingsRoot.get("player").get("id"), playerRoot.get("id")));

        userSubquery.select(userSettingsRoot.get("player").get("email"));

        userSubquery.where(criteriaBuilder.lessThan(criteriaBuilder.currentDate(),userSettingsRoot.get("expirationDate")));

        return userSubquery;
    }

    private Subquery<String> generateSubqueryRejectedParticipants(CriteriaBuilder criteriaBuilder, CriteriaQuery<?> criteriaQuery, Root<Player> playerRoot) {
        Subquery<String> userSubquery = criteriaQuery.subquery(String.class);
        Root<UserSettings> userSettingsRoot = userSubquery.from(UserSettings.class);

        Join<UserSettings, User> userJoin = userSettingsRoot.join("player", JoinType.INNER);
        userJoin.on(criteriaBuilder.equal(userSettingsRoot.get("player").get("id"), playerRoot.get("id")));

        userSubquery.select(userSettingsRoot.get("player").get("email"));

        userSubquery.where(criteriaBuilder.lessThan(userSettingsRoot.get("expirationDate"), criteriaBuilder.currentDate()));

        return userSubquery;
    }


    private List<Predicate> getFilterPredicates(ParticipantSearchCriteria searchCriteria, CriteriaBuilder builder, Root<Player> playerRoot) {

        Predicate emailCondition = null;
        Predicate nameCondition = null;
        Predicate scoreCondition = null;
        Predicate createdDateCondition;
        LocalDateTime createdDateStart;
        LocalDateTime createdDateEnd;
        Predicate twoScoresCondition = null;
        Predicate quickSearchCondition = null;
        Predicate invitationStatus = null;

        final CriteriaQuery<SearchParticipantsJSON> criteriaQuery = builder.createQuery(SearchParticipantsJSON.class);
        Predicate isInTournament = builder.exists(generateSubqueryAcceptedParticipants(builder, criteriaQuery, playerRoot));
        Predicate isPlayerInvitationInPending = builder.exists(generateSubqueryPendingParticipants(builder,criteriaQuery,playerRoot));
        Predicate isPlayerInvitationRejected = builder.exists(generateSubqueryRejectedParticipants(builder, criteriaQuery, playerRoot));

        if(searchCriteria.getEmail() != null) {
            emailCondition = builder.equal(playerRoot.get("email"), searchCriteria.getEmail());
        }

        //Search by firstname
        if (searchCriteria.getName() != null) {
            nameCondition = builder.equal(playerRoot.get("firstName"), searchCriteria.getName());
        }

        if(searchCriteria.getScore() != null) {
            scoreCondition = builder.equal(playerRoot.get("score"), searchCriteria.getScore());
        }

        if (searchCriteria.getMinScore() != null && searchCriteria.getMaxScore() != null) {
            twoScoresCondition = builder.between(playerRoot.get("score"), searchCriteria.getMinScore(), searchCriteria.getMaxScore());
        } else if (searchCriteria.getMinScore() == null && searchCriteria.getMaxScore() != null) {
            twoScoresCondition = builder.lessThanOrEqualTo(playerRoot.get("score"), searchCriteria.getMaxScore());
        } else if (searchCriteria.getMinScore() != null && searchCriteria.getMaxScore() == null) {
            twoScoresCondition = builder.greaterThanOrEqualTo(playerRoot.get("score"), searchCriteria.getMinScore());
        }

        if (searchCriteria.getQuickSearch() != null) {
            Predicate qsEmailCondition = builder.like(builder.lower(playerRoot.get("email")),
                    "%" + searchCriteria.getQuickSearch().toLowerCase() + "%");
            Predicate qsFirstnameCondition = builder.like(builder.lower(playerRoot.get("firstName")),
                    "%" + searchCriteria.getQuickSearch().toLowerCase() + "%");
            Predicate qsLastnameCondition = builder.like(builder.lower(playerRoot.get("lastName")),
                    "%" + searchCriteria.getQuickSearch().toLowerCase() + "%");

            quickSearchCondition = builder.or(qsEmailCondition, qsFirstnameCondition, qsLastnameCondition);
        }

        //Search by creation date
        createdDateStart = searchCriteria.getCreatedDateStart() != null ? DateUtil.stringToDate(searchCriteria.getCreatedDateStart()) : START_DATE_SENTINEL;
        createdDateEnd = searchCriteria.getCreatedDateEnd() != null ? DateUtil.stringToDate(searchCriteria.getCreatedDateEnd()) : END_DATE_SENTINEL;

        createdDateCondition = builder.between(
                playerRoot.get("createdDate"),
                createdDateStart,
                createdDateEnd
        );

        if(searchCriteria.getInvitationStatus() != null) {
            if(searchCriteria.getInvitationStatus() == InvitationStatus.ACCEPTED) {
                invitationStatus = isInTournament;
            } else if(searchCriteria.getInvitationStatus() == InvitationStatus.PENDING){
                invitationStatus = isPlayerInvitationInPending;
            } else if (searchCriteria.getInvitationStatus() == InvitationStatus.REJECTED){
                invitationStatus = isPlayerInvitationRejected;
            }
        }

        return Stream.of(emailCondition, nameCondition, scoreCondition, twoScoresCondition, quickSearchCondition, createdDateCondition, invitationStatus)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
