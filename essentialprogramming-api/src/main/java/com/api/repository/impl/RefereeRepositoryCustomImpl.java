package com.api.repository.impl;

import com.api.entities.*;
import com.api.model.RefereeSearchCriteria;
import com.api.output.SearchRefereeJSON;
import com.api.repository.RefereeRepositoryCustom;
import com.util.enums.PlatformType;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RefereeRepositoryCustomImpl implements RefereeRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<SearchRefereeJSON> searchReferee(RefereeSearchCriteria refereeSearchCriteria) {
        final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<SearchRefereeJSON> criteriaQuery = criteriaBuilder.createQuery(SearchRefereeJSON.class);
        final Root<User> userRoot = criteriaQuery.from(User.class);

        Subquery<Long> totalMatches = generateSubqueryForCountAssignedMatchesToReferee(criteriaBuilder,criteriaQuery,userRoot);

        criteriaQuery.select(criteriaBuilder.construct(
                SearchRefereeJSON.class,
                userRoot.get("userKey"),
                userRoot.get("firstName"),
                userRoot.get("lastName"),
                userRoot.get("email"),
                totalMatches.getSelection()
                ))
                .groupBy(userRoot.get("userKey"), userRoot.get("id"));

        List<Predicate> predicates = getFilterPredicates(refereeSearchCriteria, criteriaBuilder, userRoot);
        if(!predicates.isEmpty()) {
            criteriaQuery.where(predicates.toArray(new Predicate[]{}));
        }

        return entityManager.createQuery(criteriaQuery).getResultList();
    }

    private Subquery<Long> generateSubqueryForCountAssignedMatchesToReferee(CriteriaBuilder criteriaBuilder, CriteriaQuery<?> criteriaQuery, Root<User> userRoot) {
        Subquery<Long> subquery = criteriaQuery.subquery(Long.class);
        Root<Match> matchRoot = subquery.from(Match.class);

        Predicate refereeConition = criteriaBuilder.equal(matchRoot.get("referee").get("id"), userRoot.get("id"));

        subquery.select(criteriaBuilder.count(matchRoot)).where(refereeConition);

        return subquery;
    }

    private List<Predicate> getFilterPredicates(RefereeSearchCriteria searchCriteria, CriteriaBuilder criteriaBuilder, Root<User> userRoot) {
        Predicate nameCondition = null;

        Join<User, UserPlatform> userPlatformJoin = userRoot.join("userPlatformList", JoinType.INNER);
        Predicate platformTypeRefereeCondition = criteriaBuilder.equal(userPlatformJoin.get("platformType"), PlatformType.REFEREE);

        if(searchCriteria.getName() != null) {
            nameCondition = criteriaBuilder.equal(userRoot.get("firstName"), searchCriteria.getName());
        }

        return Stream.of(nameCondition, platformTypeRefereeCondition)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
