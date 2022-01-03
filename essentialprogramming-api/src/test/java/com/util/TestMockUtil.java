package com.util;

import com.api.service.WebSocketManager;
import com.crypto.Crypt;
import com.internationalization.Messages;
import com.spring.ApplicationContextFactory;
import com.util.enums.Language;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.ArgumentMatchers.any;

public class TestMockUtil {

    public static MockedStatic<ApplicationContextFactory> mockStaticACF() {

        MockedStatic<ApplicationContextFactory> mockStaticACF = Mockito.mockStatic(ApplicationContextFactory.class);
        mockStaticACF.when(() -> ApplicationContextFactory.getBean("messageSource", ReloadableResourceBundleMessageSource.class)).thenReturn(null);
        return mockStaticACF;

    }

    public static MockedStatic<Messages> mockStaticMessages(String messageId, String errorMessage) {

        MockedStatic<Messages> mockedStaticMessages = Mockito.mockStatic(Messages.class);
        mockedStaticMessages.when(() -> Messages.get(messageId, Language.ENGLISH)).thenReturn(errorMessage);
        return mockedStaticMessages;

    }

    public static MockedStatic<WebSocketManager> mockStaticWSM() {

        MockedStatic<WebSocketManager> wsMockStatic = Mockito.mockStatic(WebSocketManager.class);
        wsMockStatic.when(() -> WebSocketManager.sendMessage(anyString())).thenAnswer((Answer<Void>) invocation -> null);
        return wsMockStatic;
    }

    public static MockedStatic<Crypt> mockCrypt() {
        MockedStatic<Crypt> mockedCryptStatic = Mockito.mockStatic(Crypt.class);

        mockedCryptStatic.when(() -> Crypt.encrypt(any(), any())).thenReturn(null);

        return mockedCryptStatic;
    }

}