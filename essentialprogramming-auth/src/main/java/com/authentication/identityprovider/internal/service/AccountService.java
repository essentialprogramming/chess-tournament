package com.authentication.identityprovider.internal.service;

import com.authentication.identityprovider.internal.entities.*;
import com.authentication.identityprovider.AuthenticationProvider;
import com.authentication.identityprovider.internal.model.PasswordInput;
import com.authentication.identityprovider.internal.model.ResetPasswordInput;
import com.authentication.identityprovider.internal.repository.*;
import com.authentication.request.AuthRequest;
import com.crypto.Crypt;
import com.crypto.PasswordHash;
import com.resources.AppResources;
import com.util.password.PasswordException;
import com.internationalization.Messages;
import com.util.enums.HTTPCustomStatus;
import com.util.enums.Language;
import com.util.exceptions.ApiException;
import com.util.password.PasswordStrength;
import com.util.password.PasswordUtil;
import com.util.web.JsonResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.email.service.EmailManager;

import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.util.*;


@Service
public class AccountService implements AuthenticationProvider {

    private final AccountRepository accountRepository;
    private final EmailManager emailManager;

    @Autowired
    public AccountService(AccountRepository accountRepository,
                          EmailManager emailManager) {
        this.accountRepository = accountRepository;
        this.emailManager = emailManager;
    }

    /**
     * Authenticate with the email and password .
     *
     * @param authRequest AuthRequest
     * @return Account for the email after authentication.
     */
    @Override
    public Account authenticate(AuthRequest authRequest, Language language) throws ApiException {

        boolean isValidPassword;
        Optional<Account> account = getAccount(authRequest.getEmail());
        if (account.isPresent()) {
            if (account.get().isDeleted()) {
                throw new ApiException(Messages.get("USER.ACCOUNT.DELETED", language), HTTPCustomStatus.BUSINESS_EXCEPTION);
            }

            isValidPassword = PasswordHash.matches(authRequest.getPassword(), account.get().getPassword());
        } else {
            throw new ApiException(Messages.get("USER.NOT.EXIST", language), HTTPCustomStatus.BUSINESS_EXCEPTION);
        }
        if (isValidPassword) {
            return account.get();
        }
        throw new ApiException(Messages.get("USER.PASSWORD.INVALID", language), HTTPCustomStatus.BUSINESS_EXCEPTION);
    }

    private Optional<Account> getAccount(String email) {
        return accountRepository.findByEmail(email);
    }

    public Serializable setPassword(PasswordInput passwordInput, Language language) throws GeneralSecurityException, ApiException, PasswordException {

        String decryptedUserKey = Crypt.decrypt(passwordInput.getKey(), AppResources.ENCRYPTION_KEY.value());
        Optional<Account> account = accountRepository.findByUserKey(decryptedUserKey);

        if (!account.isPresent()) {
            throw new ApiException(Messages.get("USER.NOT.EXIST", Language.ENGLISH), HTTPCustomStatus.BUSINESS_EXCEPTION);
        }
        if (account.get().isDeleted()) {
            throw new ApiException(Messages.get("USER.ACCOUNT.DELETED", language), HTTPCustomStatus.BUSINESS_EXCEPTION);
        }

        if (!passwordInput.getNewPassword().equals(passwordInput.getConfirmPassword())) {
            throw new ApiException(Messages.get("USER.PASSWORD.DONT.MATCH", language), HTTPCustomStatus.BUSINESS_EXCEPTION);
        }

        PasswordStrength passwordStrength = PasswordUtil.getPasswordStrength(passwordInput.getNewPassword());
        boolean isStrongPassword = PasswordUtil.isStrongPassword(passwordInput.getNewPassword());

        if (!isStrongPassword)
            throw new PasswordException(Messages.get("USER.PASSWORD.STRENGTH", language), PasswordStrength.get(passwordStrength.getValue()));

        account.ifPresent(user -> user.setPassword(PasswordHash.encode(passwordInput.getNewPassword())));


        return new JsonResponse()
                .with("status", "ok")
                .done();
    }


    @Override
    public Serializable resetPassword(ResetPasswordInput resetPasswordInput, Language language) throws ApiException {
        return null;
    }


}
