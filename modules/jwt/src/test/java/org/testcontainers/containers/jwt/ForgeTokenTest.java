package org.testcontainers.containers.jwt;

import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.auth0.jwt.interfaces.RSAKeyProvider;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static java.time.LocalDateTime.now;

public class ForgeTokenTest {
    private static JWTVerifier verifier;
    private static JwtContainer jwtContainer;

    @BeforeClass
    public static void before() {
        jwtContainer = new JwtContainer();
        jwtContainer.start();

        // Public Key
        JwkProvider provider = new JwkProviderBuilder(jwtContainer.issuer()).build();
        RSAKeyProvider keyProvider = createKeyProvider(provider);
        verifier = JWT.require(Algorithm.RSA256(keyProvider))
            .withIssuer(jwtContainer.issuer().toString()).build();
    }

    @AfterClass
    public static void after() {
        jwtContainer.stop();
    }


    @Test
    public void should_forge_token_with_expected_content() {
        LocalDateTime now = now(ZoneId.systemDefault());
        LocalDateTime expiration = now.plusMinutes(10);

        TokenForgery tokenForgery = jwtContainer.forgery();
        tokenForgery
            .withAudience("audience")
            .withJWTId("jwtId")
            .withSubject("subject")
            .withArrayClaims("scp", "SCOPE_1", "SCOPE_2")
            .expiresAt(expiration)
            .issuedAt(now)
            .notBefore(now);

        String token = tokenForgery.forge();
    }


    private static Date convert(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).withNano(0).toInstant());

    }

    private static RSAKeyProvider createKeyProvider(JwkProvider provider) {
        return new RSAKeyProvider() {
            @Override
            public RSAPublicKey getPublicKeyById(String keyId) {
                PublicKey publicKey;
                try {
                    publicKey = provider.get(keyId).getPublicKey();
                } catch (JwkException ex) {
                    throw new JWTVerificationException("Error while retrieving public key gor keyId: " + keyId, ex);
                }
                return (RSAPublicKey) publicKey;
            }

            @Override
            public RSAPrivateKey getPrivateKey() {
                return null;
            }

            @Override
            public String getPrivateKeyId() {
                return null;
            }
        };
    }
}
