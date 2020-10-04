package org.testcontainers.containers.jwt;

import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class ForgeTokenTest {
    private static JWTVerifier verifier;

    // declaration {
    private static JwtContainer jwtContainer;
    // }

    // junitBefore {
    @BeforeClass
    public static void before() {
        jwtContainer = new JwtContainer();
        jwtContainer.start();

        // }
        // Public Key
        JwkProvider provider = new JwkProviderBuilder(jwtContainer.issuer()).build();
        RSAKeyProvider keyProvider = createKeyProvider(provider);
        verifier = JWT.require(Algorithm.RSA256(keyProvider))
            .withIssuer(jwtContainer.issuer().toString()).build();
    }

    // junitAfter {
    @AfterClass
    public static void after() {
        jwtContainer.stop();
    }
    // }

    @Test
    public void should_forge_token_with_expected_content() {
        LocalDateTime now = now(ZoneId.systemDefault());
        LocalDateTime expiration = now.plusMinutes(10);

        // forge {
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
        // }


        DecodedJWT jwt = verifier.verify(token);
        assertThat(jwt.getIssuer(), is(jwtContainer.issuer().toString()));
        assertThat(jwt.getAudience(), contains("audience"));
        assertThat(jwt.getId(), is("jwtId"));
        assertThat(jwt.getSubject(), is("subject"));
        assertThat(jwt.getClaim("scp").asList(String.class), contains("SCOPE_1", "SCOPE_2"));

        assertThat(jwt.getExpiresAt(), equalTo(convert(expiration)));
        assertThat(jwt.getNotBefore(), is(convert(now)));
        assertThat(jwt.getIssuedAt(), is(convert(now)));

        assertThat(jwt.getKeyId(), is("test"));
        assertThat(jwt.getClaims().size(), is(8));
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
