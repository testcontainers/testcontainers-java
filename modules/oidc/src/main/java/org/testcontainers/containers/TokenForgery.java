package org.testcontainers.containers;


import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public final class TokenForgery {

    private static final String PUB_KEY_PATH = "/org/testcontainers/jwks/public_key.der";
    private static final String PRI_KEY_PATH = "/org/testcontainers/jwks/private_key.der";
    private static final String KEY_ID = "test";

    private final Map<String, String[]> claims;
    private final String issuer;

    private String[] audience;
    private String jwtId;
    private String subject;
    private Date expiresAt;
    private Date issuedAt;
    private Date notBefore;

    public TokenForgery(String issuer) {
        this.issuer = issuer;
        this.audience = new String[0];
        this.claims = new HashMap<>();
    }

    public final TokenForgery withAudience(String... audience) {
        this.audience = audience;
        return this;
    }

    public final TokenForgery withJWTId(String jwtId) {
        this.jwtId = jwtId;
        return this;
    }

    public final TokenForgery withSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public final TokenForgery withArrayClaims(String key, String... scopes) {
        this.claims.put(key, scopes);
        return this;
    }

    public final TokenForgery expiresAt(LocalDateTime expiresAt) {
        this.expiresAt = Date.from(expiresAt.atZone(ZoneId.systemDefault()).toInstant());
        return this;
    }

    public final TokenForgery issuedAt(LocalDateTime issuedAt) {
        this.issuedAt = Date.from(issuedAt.atZone(ZoneId.systemDefault()).toInstant());
        return this;
    }

    public final TokenForgery notBefore(LocalDateTime notBefore) {
        this.notBefore = Date.from(notBefore.atZone(ZoneId.systemDefault()).toInstant());
        return this;
    }

    public final String forge() {
        Algorithm algorithm = createAlgorithm();
        JWTCreator.Builder jwtCreator = JWT.create()
            .withIssuer(issuer)
            .withKeyId(KEY_ID)
            .withAudience(audience)
            .withJWTId(jwtId)
            .withSubject(subject)
            .withExpiresAt(expiresAt)
            .withNotBefore(notBefore)
            .withIssuedAt(issuedAt);
        claims.forEach(jwtCreator::withArrayClaim);
        return jwtCreator.sign(algorithm);
    }

    @SneakyThrows
    private Algorithm createAlgorithm() {
        InputStream publicKeyStream = this.getClass().getResourceAsStream(PUB_KEY_PATH);
        Objects.requireNonNull(publicKeyStream);
        byte[] publicKeyBytes = IOUtils.toByteArray(publicKeyStream);
        X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(publicKeyBytes);
        RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(publicSpec);
        Objects.requireNonNull(publicKey);

        InputStream privateKeyStream = this.getClass().getResourceAsStream(PRI_KEY_PATH);
        Objects.requireNonNull(privateKeyStream);
        byte[] privateKeyBytes = IOUtils.toByteArray(privateKeyStream);
        PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        RSAPrivateKey privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(privateSpec);
        Objects.requireNonNull(privateKey);

        return Algorithm.RSA256(publicKey, privateKey);
    }
}
