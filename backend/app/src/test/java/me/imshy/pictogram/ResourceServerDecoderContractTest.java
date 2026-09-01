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

class ResourceServerDecoderContractTest {

    private static final String ISSUER = "pictogram";
    private static final ECKey SIGNING_KEY = generateSigningKey();
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
