package org.testcontainers.containers.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import org.apache.commons.io.IOUtils;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static java.time.ZoneId.systemDefault;

public class TokenForgery {
    private static final String PUB_KEY_PATH = "public_key.der";
    private static final String PRI_KEY_PATH = "private_key.der";
    private static final String KEY_ID = "test";

    private final String issuer;
    private String[] audience;
    private String jwtId;
    private String subject;

    private Date expiresAt;
    private Date issuedAt;
    private Date notBefore;

    private final Map<String, String[]> claims = new HashMap<>();

    public TokenForgery(String issuer) {
        this.issuer = issuer;
    }

    public TokenForgery withAudience(String... audience) {
        this.audience = audience;
        return this;
    }

    public TokenForgery withJWTId(String jwtId) {
        this.jwtId = jwtId;
        return this;
    }

    public TokenForgery withSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public TokenForgery withArrayClaims(String key, String... scopes) {
        claims.put(key, scopes);
        return this;
    }

    public TokenForgery expiresAt(LocalDateTime expiresAt) {
        this.expiresAt = Date.from(expiresAt.atZone(systemDefault()).toInstant());
        return this;
    }

    public TokenForgery issuedAt(LocalDateTime issuedAt) {
        this.issuedAt = Date.from(issuedAt.atZone(systemDefault()).toInstant());
        return this;
    }

    public TokenForgery notBefore(LocalDateTime notBefore) {
        this.notBefore = Date.from(notBefore.atZone(systemDefault()).toInstant());
        return this;
    }

    public String forge() {
        Algorithm algorithm;

        try {
            // Public Key
            byte[] publicKeyBytes = IOUtils.toByteArray(this.getClass().getResourceAsStream(PUB_KEY_PATH));
            X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(publicKeyBytes);
            RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(publicSpec);

            // Private Key
            byte[] privateKeyBytes = IOUtils.toByteArray(this.getClass().getResourceAsStream(PRI_KEY_PATH));
            PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            RSAPrivateKey privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(privateSpec);

            algorithm = Algorithm.RSA256(publicKey, privateKey);
        } catch (Exception ex) {
            throw new RuntimeException("Unable to create algorithm", ex);
        }

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
}
