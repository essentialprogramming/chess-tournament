package com.api.service;


import com.api.entities.Player;
import com.api.entities.User;
import com.api.mapper.PlayerMapper;
import com.api.model.UserInput;
import com.api.output.PlayerJSON;
import com.api.entities.UserPlatform;
import com.api.output.UserJSON;
import com.api.repository.PlayerRepository;
import com.api.repository.UserPlatformRepository;
import com.api.repository.UserRepository;
import com.crypto.Crypt;
import com.internationalization.Messages;
import com.spring.ApplicationContextFactory;
import com.util.TestEntityGenerator;
import com.util.TestMockUtil;
import com.util.enums.Language;
import com.util.enums.PlatformType;
import com.util.exceptions.ApiException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.GeneralSecurityException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private UserPlatformService userPlatformService;

    public MockedStatic<ApplicationContextFactory> acfMockStatic;

    public MockedStatic<Messages> messagesMockedStatic;

    public MockedStatic<Crypt> mockedCryptStatic;

    @AfterEach
    public void afterEach() {

        if (acfMockStatic != null) {
            acfMockStatic.close();
        }

        if (messagesMockedStatic != null) {
            messagesMockedStatic.close();
        }

        if (mockedCryptStatic != null) {
            mockedCryptStatic.close();
        }
    }

    @Test
    public void getUserSuccessfully(){

        //given
        User user = TestEntityGenerator.generateUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        //when
        UserJSON result = userService.loadUser(user.getEmail(), Language.ENGLISH);

        //then
        Assertions.assertThat(result.getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    public void loadUserNotExists() {

        //given
        User user = TestEntityGenerator.generateUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());

        acfMockStatic = TestMockUtil.mockStaticACF();
        messagesMockedStatic = TestMockUtil.mockStaticMessages("USER.NOT.FOUND", "user does not exists");

        //when
        Throwable throwable = catchThrowable(() -> userService.loadUser(user.getEmail(), Language.ENGLISH));

        //then
        Assertions.assertThat(throwable).isInstanceOf(ApiException.class).hasMessage("user does not exists");
    }

    @Test
    public void saveUserSuccessfully() throws GeneralSecurityException {

        //given
        UserInput userInput = TestEntityGenerator.generateUserInput();
        when(playerRepository.save(any())).thenReturn(PlayerMapper.inputToPlayer(userInput));
        when(userPlatformService.addPlatform(PlatformType.PLAYER, userInput.getEmail()))
                .thenReturn(PlayerMapper.playerToJson(PlayerMapper.inputToPlayer(userInput)));

        //when
        PlayerJSON result = userService.saveUser(userInput, PlatformType.PLAYER);

        //then
        assertThat(result.getEmail()).isEqualTo(userInput.getEmail());
        assertThat(result.getFirstName()).isEqualTo(userInput.getFirstName());
        assertThat(result.getLastName()).isEqualTo(userInput.getLastName());
        assertThat(result.getScore()).isEqualTo(0);
    }

    @Test
    public void savePlayerSuccessfully() throws GeneralSecurityException {

        //given
        UserInput userInput = TestEntityGenerator.generateUserInput();

        mockedCryptStatic = TestMockUtil.mockCrypt();

        when(playerRepository.save(any())).thenReturn(null);
        when(userPlatformService.addPlatform(any(), any())).thenReturn(null);

        //when
        PlayerJSON result =  userService.saveUser(userInput, PlatformType.PLAYER);

        //then
        assertThat(result.getEmail()).isEqualTo(userInput.getEmail());
        assertThat(result.getFirstName()).isEqualTo(userInput.getFirstName());
        assertThat(result.getLastName()).isEqualTo(userInput.getLastName());
        assertThat(result.getScore()).isEqualTo(0D);

        verify(userPlatformService).addPlatform(any(), any());
        verify(playerRepository).save(any());
    }

    @Test
    public void savePlayerSuccessfullyUserPlatformServiceCall() throws GeneralSecurityException {

        //given
        UserInput userInput = TestEntityGenerator.generateUserInput();

        mockedCryptStatic = TestMockUtil.mockCrypt();

        when(playerRepository.save(any())).thenReturn(null);

        //when
        PlayerJSON result =  userService.saveUser(userInput, PlatformType.PLAYER);

        //then
        assertThat(result.getEmail()).isEqualTo(userInput.getEmail());
        assertThat(result.getFirstName()).isEqualTo(userInput.getFirstName());
        assertThat(result.getLastName()).isEqualTo(userInput.getLastName());
        assertThat(result.getScore()).isEqualTo(0D);

        verify(userPlatformService).addPlatform(any(), any());
        verify(playerRepository).save(any());
        verify(userPlatformService).addPlatform(PlatformType.PLAYER, userInput.getEmail());
    }
}
