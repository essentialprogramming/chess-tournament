package com.api.repository;

import com.api.entities.User;
import com.api.output.SearchRefereeJSON;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    @Query("SELECT distinct new com.api.output.SearchRefereeJSON(users.userKey, users.firstName, users.lastName, users.email, "
            + " (SELECT COUNT(match.id) FROM match match "
            + " WHERE match.referee.id = users.id)) " + " FROM user users " + " INNER JOIN user_platform userplatform "
            + " on users.id = userplatform.user.id " + " WHERE userplatform.platformType = 'REFEREE' ")
    List<SearchRefereeJSON> findAllReferees();

    Optional<User> findByEmail(String email);

    Optional<User> findByUserKey(String userKey);
}
