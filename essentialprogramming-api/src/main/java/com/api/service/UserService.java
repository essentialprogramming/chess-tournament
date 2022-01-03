package com.api.service;

import com.api.entities.*;
import com.api.mapper.PlayerMapper;
import com.api.mapper.UserMapper;
import com.api.model.*;
import com.api.output.PlayerJSON;
import com.api.output.UserJSON;
import com.api.repository.*;
import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.crypto.PasswordHash;
import com.internationalization.Messages;
import com.util.enums.HTTPCustomStatus;
import com.util.enums.PlatformType;
import com.util.exceptions.ApiException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);


    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;
    private final UserPlatformRepository userPlatformRepository;
    private final UserPlatformService userPlatformService;


    @Transactional
    public boolean checkAvailabilityByEmail(String email) {

        Optional<User> user = userRepository.findByEmail(email);
        return !user.isPresent();
    }


    @Transactional
    public UserJSON loadUser(String email, com.util.enums.Language language) throws ApiException {
        Optional<User> user = userRepository.findByEmail(email);

        if (user.isPresent()) {
            logger.info("User loaded={}",email);
            return UserMapper.userToJson(user.get());
        } else
            throw new ApiException(Messages.get("USER.NOT.FOUND", language), HTTPCustomStatus.INVALID_REQUEST);

    }


    @Transactional
    public PlayerJSON saveUser(UserInput input, PlatformType platform) throws GeneralSecurityException {

        Player player = PlayerMapper.inputToPlayer(input);
        Player result = saveUser(player, input, platform);

        return PlayerMapper.playerToJson(result);

    }


    private Player saveUser(Player player, UserInput input, PlatformType platform) throws GeneralSecurityException {

        String playerKey = NanoIdUtils.randomNanoId();

        //set created date
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String formatDateTime = now.format(formatter);
        LocalDateTime formatDatTime = LocalDateTime.parse(formatDateTime, formatter);
        player.setCreatedDate(formatDatTime);

        player.setUserKey(playerKey);

        player.setScore(0D);
        playerRepository.save(player);

        if (player.getId() > 0) {
            logger.debug("Start password hashing");
            String password = PasswordHash.encode(input.getPassword());
            logger.debug("Finished password hashing");

            player.setPassword(password);
        }

        userPlatformService.addPlatform(platform, player.getEmail());


        return player;
    }

}
