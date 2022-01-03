package com.api.repository;

import com.api.entities.User;
import com.api.entities.UserPlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPlatformRepository extends JpaRepository<UserPlatform, Integer> {
    void deleteAllByUserIn(List<User> user);
}
