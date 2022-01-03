package com.api.service;

import com.api.entities.User;
import com.api.entities.UserPlatform;
import com.api.output.PlayerJSON;
import com.api.repository.UserPlatformRepository;
import com.api.repository.UserRepository;
import com.util.TestEntityGenerator;
import com.util.enums.PlatformType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;

import java.util.Collections;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserPlatformServiceTest {

    @InjectMocks
    private UserPlatformService userPlatformService;

    @Mock
    private UserPlatformRepository userPlatformRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    public void addPlatformSuccessfully() {

        //given
        User user = TestEntityGenerator.generateUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userPlatformRepository.save(any())).thenReturn(any(UserPlatform.class));

        //when
        PlayerJSON result = userPlatformService.addPlatform(PlatformType.PLAYER, user.getEmail());

        //then
        assertThat(result.getEmail()).isEqualTo(user.getEmail());
        assertThat(result.getFirstName()).isEqualTo(user.getFirstName());
        assertThat(result.getLastName()).isEqualTo(user.getLastName());
    }

    @Test
    public void platformIsAvailable() {

        //given
        User user = TestEntityGenerator.generateUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        //when
        boolean isAvailable = userPlatformService.isPlatformAvailable(PlatformType.PLAYER, user.getEmail());

        //then
        assertThat(isAvailable).isTrue();
    }

    @Test
    public void platformIsNotAvailable() {

        //given
        User user = TestEntityGenerator.generateUser();
        user.setUserPlatformList(Collections.singletonList(TestEntityGenerator.generateUserPlatform(PlatformType.ADMIN)));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        //when
        boolean isAvailable = userPlatformService.isPlatformAvailable(PlatformType.ADMIN, user.getEmail());

        //then
        assertThat(isAvailable).isFalse();
    }
}
