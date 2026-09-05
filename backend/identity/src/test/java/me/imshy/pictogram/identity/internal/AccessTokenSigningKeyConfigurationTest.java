package me.imshy.pictogram.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import me.imshy.pictogram.identity.internal.accesstoken.SigningKey;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AccessTokenSigningKeyConfigurationTest {

    private static final String PRIVATE_JWK = generatePrivateJwk();

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(AccessTokenSigningKeyConfiguration.class);

    @Test
    void prodProfileWithoutASigningKeyFailsToStart() {
        runner.withPropertyValues("spring.profiles.active=prod").run(
            context -> assertThat(context).hasFailed().getFailure().hasMessageContaining("pictogram.auth.signing-key"));
    }

    @Test
    void prodProfileWithASigningKeyStarts() {
        runner.withPropertyValues("spring.profiles.active=prod", "pictogram.auth.signing-key=" + PRIVATE_JWK)
            .run(context -> assertThat(context).hasNotFailed().hasSingleBean(SigningKey.class));
    }

    @Test
    void withoutTheProdProfileAMissingSigningKeyFallsBackToAnEphemeralKey() {
        runner.run(context -> assertThat(context).hasNotFailed().hasSingleBean(SigningKey.class));
    }

    private static String generatePrivateJwk() {
        try {
            return new ECKeyGenerator(Curve.P_256).generate().toJSONString();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
