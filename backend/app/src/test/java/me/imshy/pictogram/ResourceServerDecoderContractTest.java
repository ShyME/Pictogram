package me.imshy.pictogram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.time.Instant;
import java.util.UUID;
import me.imshy.pictogram.identity.PictogramAccessTokens;
import me.imshy.pictogram.testsupport.SharedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * A stray {@code spring.security.oauth2.resourceserver.jwt.*} property must not knock out
 * identity's {@link JwtDecoder} (ADR-0004, #28). {@code issuer-uri} and {@code jwk-set-uri}
 * arm Spring Boot's competing decoder through different conditions, so each is exercised in
 * its own context: the app must still start and identity's decoder must be the one the
 * {@code /api/**} resource server verifies with.
 */
class ResourceServerDecoderContractTest {

    private static final String ISSUER = "pictogram";
    private static final ECKey SIGNING_KEY = generateSigningKey();
    // An arbitrary value; enough for Boot to arm a competing JwtDecoder, never reached.
    private static final String ARBITRARY_URI = "https://accounts.example.test/not-pictogram";

    @SpringBootTest(classes = PictogramApplication.class)
    @AutoConfigureMockMvc
    @ActiveProfiles("test")
    static class WithIssuerUri extends Fixture {

        @DynamicPropertySource
        static void properties(DynamicPropertyRegistry registry) {
            commonProperties(registry);
            registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> ARBITRARY_URI);
        }
    }

    @SpringBootTest(classes = PictogramApplication.class)
    @AutoConfigureMockMvc
    @ActiveProfiles("test")
    static class WithJwkSetUri extends Fixture {

        @DynamicPropertySource
        static void properties(DynamicPropertyRegistry registry) {
            commonProperties(registry);
            registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> ARBITRARY_URI + "/jwks");
        }
    }

    abstract static class Fixture {

        @Autowired
        MockMvc mvc;

        @Autowired
        JwtDecoder resourceServerJwtDecoder;

        @Autowired
        PictogramAccessTokens accessTokens;

        static void commonProperties(DynamicPropertyRegistry registry) {
            SharedPostgres.registerTo(registry);
            registry.add("pictogram.auth.signing-key", SIGNING_KEY::toJSONString);
        }

        @Test
        void identitysDecoderIsTheOneWiredIntoTheApiResourceServer() {
            var user = UUID.randomUUID();
            String token = mintPictogramAccessToken(user);

            assertThat(resourceServerJwtDecoder.decode(token).getSubject()).isEqualTo(user.toString());
            assertThat(accessTokens.resolve(token).value()).isEqualTo(user);
        }

        @Test
        void theApiChainAcceptsAPictogramTokenAndRejectsEverythingElse() throws Exception {
            String token = mintPictogramAccessToken(UUID.randomUUID());

            // Accepted: authenticated but not onboarded, so the endpoint answers 404 — not 401.
            mvc.perform(get("/api/profiles/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isNotFound());

            mvc.perform(get("/api/profiles/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-pictogram-token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    private static String mintPictogramAccessToken(UUID user) {
        Instant now = Instant.now();
        var encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(SIGNING_KEY)));
        var claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.ES256).build(), claims)).getTokenValue();
    }

    private static ECKey generateSigningKey() {
        try {
            return new ECKeyGenerator(Curve.P_256).keyID("pictogram-test-signing-key").generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
