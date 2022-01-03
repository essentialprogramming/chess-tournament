package com.authentication.service;

import com.authentication.exceptions.codes.ErrorCode;
import com.authentication.security.KeyStoreService;
import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.nimbusds.jwt.SignedJWT;
import com.util.exceptions.ServiceException;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Service
public class TokenService {
    private static final Logger LOG = LoggerFactory.getLogger(TokenService.class);

    //Always RSA 256, but could be parametrized
    private final JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256)
            .type(JOSEObjectType.JWT)
            .build();

    private final KeyStoreService keyStoreService;

    @Autowired
    public TokenService(KeyStoreService keyStoreService) {
        this.keyStoreService = keyStoreService;
    }


    /**
     * Generates an OAuth Token and returns it.
     *
     * @param privateClaimTypeAndString      The map of private claims where the values are single strings
     * @param privateClaimTypeAndStringArray The map of private claims where the values are string arrays
     * @return The OAuth token
     * @throws ServiceException thrown exception
     */
    public String generateJwtToken(final long expiresIn,
                                   final Map<String, String> privateClaimTypeAndString,
                                   final Map<String, String[]> privateClaimTypeAndStringArray)
            throws ServiceException {
        LOG.info("creating signed JWT token");

        // Create the payload
        final Payload payload = createTokenPayload(expiresIn, privateClaimTypeAndString,
                privateClaimTypeAndStringArray);

        // Create JWT token containing the payload
        return createSignedToken(payload, keyStoreService.getPrivateKey());
    }


    private Payload createTokenPayload(long expiresIn,
                                       Map<String, String> privateClaimTypeAndString,
                                       Map<String, String[]> privateClaimTypeAndStringArray) {

        Instant now = Instant.now();
        Date expirationTime = Date.from(now.plus(expiresIn, ChronoUnit.MINUTES));

        //3. JWT Payload or claims
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .issuer("Authorization Service")
                .audience("Chess Services")
                .expirationTime(expirationTime) // expires in 30 minutes
                .notBeforeTime(Date.from(now))
                .issueTime(Date.from(now))
                .jwtID(NanoIdUtils.randomNanoId());

        if (privateClaimTypeAndString != null) {
            for (Map.Entry<String, String> entry : privateClaimTypeAndString.entrySet()) {
                builder.claim(entry.getKey(), entry.getValue());
            }
        }

        if (privateClaimTypeAndStringArray != null) {
            for (Map.Entry<String, String[]> entry : privateClaimTypeAndStringArray.entrySet()) {
                builder.claim(entry.getKey(), entry.getValue());
            }
        }

        JWTClaimsSet jwtClaims = builder.build();
        // return payload
        return new Payload(jwtClaims.toJSONObject());
    }


    private String createSignedToken(Payload payload, PrivateKey privateSigningKey)
            throws ServiceException {

        if (privateSigningKey == null) {
            throw new ServiceException(ErrorCode.PRIVATE_KEY_IS_NULL);
        }

        JWSObject jwsObject = new JWSObject(jwsHeader, payload);

        try {
            JWSSigner signer = new RSASSASigner(privateSigningKey, false);
            jwsObject.sign(signer);
        } catch (JOSEException e) {
            LOG.error(ErrorCode.UNABLE_TO_SIGN_TOKEN.getDescription());
            throw new ServiceException(ErrorCode.UNABLE_TO_SIGN_TOKEN, e);
        }

        return jwsObject.serialize();
    }



    protected String getRefreshToken(String identifier) {
        JWSSigner jwsSigner = new RSASSASigner(keyStoreService.getPrivateKey(), true);
        Instant now = Instant.now();

        JWTClaimsSet refreshTokenClaims = new JWTClaimsSet.Builder()
                .issuer("EssentialProgramming Auth Service")
                .claim("identifier", identifier)
                //refresh token for 1 day.
                .expirationTime(Date.from(now.plus(1, ChronoUnit.DAYS)))
                .build();
        SignedJWT signedRefreshToken = new SignedJWT(jwsHeader, refreshTokenClaims);
        try {
            signedRefreshToken.sign(jwsSigner);
        } catch (JOSEException e) {
            LOG.error(ErrorCode.UNABLE_TO_SIGN_TOKEN.getDescription());
            throw new ServiceException(ErrorCode.UNABLE_TO_SIGN_TOKEN, e);
        }
        return signedRefreshToken.serialize();
    }
}