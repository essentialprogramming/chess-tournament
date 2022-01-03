package com.api.service;

import com.api.entities.User;
import com.api.entities.UserPlatform;
import com.api.mapper.PlayerMapper;
import com.api.output.PlayerJSON;
import com.api.repository.UserPlatformRepository;
import com.api.repository.UserRepository;
import com.util.enums.PlatformType;
import com.util.web.JsonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserPlatformService {

    private final UserRepository userRepository;
    private final UserPlatformRepository userPlatformRepository;

    @Transactional
    public boolean isPlatformAvailable(PlatformType platform ,String email) {

        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
                for (UserPlatform userPlatform : user.get().getUserPlatformList()
                ) {
                    if (userPlatform.getPlatformType() == platform) {
                        return false;
                    }
                }
        }
        return true;
    }

    @Transactional
    public boolean isPlatformAvailableByKey(PlatformType platform ,String userKey) {

        User user = userRepository.findByUserKey(userKey).orElseThrow(()->new HttpClientErrorException(HttpStatus.NOT_FOUND, "User does not exist!"));
            for (UserPlatform userPlatform : user.getUserPlatformList()
            ) {
                if (userPlatform.getPlatformType().equals(platform)) {
                    return false;
                }
            }

        return true;
    }

    @Transactional
    public PlayerJSON addPlatform(PlatformType platform, String email) {

        User user = userRepository.findByEmail(email).orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "User not found!"));
        UserPlatform userPlatform = UserPlatform.builder()
                .platformType(platform)
                .roles(Collections.singletonList(String.valueOf(platform)))
                .user(user)
                .build();

        userPlatformRepository.save(userPlatform);
        return PlayerMapper.userToPlayerJson(user);
    }

    @Transactional
    public JsonResponse addPlatformByKey(PlatformType platform, String userKey) {

        User user = userRepository.findByUserKey(userKey).orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND, "User not found!"));
        UserPlatform userPlatform = UserPlatform.builder()
                .platformType(platform)
                .roles(Collections.singletonList(String.valueOf(platform)))
                .user(user)
                .build();

        userPlatformRepository.save(userPlatform);
        return new JsonResponse().with("status", "ok")
                .with("message", platform + " platform was successfully added");
    }

}
