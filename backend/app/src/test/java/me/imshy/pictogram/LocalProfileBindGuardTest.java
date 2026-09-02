package me.imshy.pictogram;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.junit.jupiter.api.Test;

class LocalProfileBindGuardTest {

    @Test
    void localProfileOnAWildcardBindIsRejected() {
        assertThatIllegalStateException()
                .isThrownBy(() -> LocalProfileBindGuard.verify(true, "0.0.0.0"))
                .withMessageContaining("loopback");
    }

    @Test
    void localProfileOnARoutableAddressIsRejected() {
        assertThatIllegalStateException().isThrownBy(() -> LocalProfileBindGuard.verify(true, "192.168.1.5"));
    }

    @Test
    void localProfileOnLoopbackIsAllowed() {
        assertThatCode(() -> LocalProfileBindGuard.verify(true, "127.0.0.1")).doesNotThrowAnyException();
        assertThatCode(() -> LocalProfileBindGuard.verify(true, "localhost")).doesNotThrowAnyException();
    }

    @Test
    void anUnsetBindAddressIsTheDeveloperDefaultAndAllowed() {
        assertThatCode(() -> LocalProfileBindGuard.verify(true, null)).doesNotThrowAnyException();
        assertThatCode(() -> LocalProfileBindGuard.verify(true, "  ")).doesNotThrowAnyException();
    }

    @Test
    void aNonLocalProfileIsNotConstrained() {
        assertThatCode(() -> LocalProfileBindGuard.verify(false, "0.0.0.0")).doesNotThrowAnyException();
    }
}
