package me.imshy.chat.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PublicSigningKeyTest {

    @Test
    void parsesAPublicEcJwk() throws Exception {
        ECKey pair = new ECKeyGenerator(Curve.P_256).keyID(UUID.randomUUID().toString()).generate();

        PublicSigningKey key = PublicSigningKey.fromJwkJson(pair.toPublicJWK().toJSONString());

        assertThat(key.jwkSource()).isNotNull();
    }

    @Test
    void rejectsAJwkThatCarriesThePrivateKey() throws Exception {
        ECKey pair = new ECKeyGenerator(Curve.P_256).keyID(UUID.randomUUID().toString()).generate();

        assertThatThrownBy(() -> PublicSigningKey.fromJwkJson(pair.toJSONString()))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("private key");
    }

    @Test
    void rejectsMalformedJson() {
        assertThatThrownBy(() -> PublicSigningKey.fromJwkJson("not json")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAMissingKey() {
        assertThatThrownBy(() -> PublicSigningKey.fromJwkJson(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PublicSigningKey.fromJwkJson(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}
