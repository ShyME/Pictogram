package me.imshy.pictogram;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.junit.jupiter.api.Test;

class LocalProfileBindGuardTest {

    @Test
    void localProfileOnAWildcardBindIsRejected() {
        assertThatIllegalStateException().isThrownBy(() -> LocalProfileBindGuard.isLocalAndLoopback(true, "0.0.0.0"))
            .withMessageContaining("loopback");
    }

    @Test
    void localProfileOnARoutableAddressIsRejected() {
        assertThatIllegalStateException()
            .isThrownBy(() -> LocalProfileBindGuard.isLocalAndLoopback(true, "192.168.1.5"));
    }

    @Test
    void localProfileOnLoopbackIsAllowed() {
        assertThatCode(() -> LocalProfileBindGuard.isLocalAndLoopback(true, "127.0.0.1")).doesNotThrowAnyException();
        assertThatCode(() -> LocalProfileBindGuard.isLocalAndLoopback(true, "localhost")).doesNotThrowAnyException();
    }

    @Test
    void anUnsetBindAddressIsTheDeveloperDefaultAndAllowed() {
        assertThatCode(() -> LocalProfileBindGuard.isLocalAndLoopback(true, null)).doesNotThrowAnyException();
        assertThatCode(() -> LocalProfileBindGuard.isLocalAndLoopback(true, "  ")).doesNotThrowAnyException();
    }

    @Test
    void aNonLocalProfileIsNotConstrained() {
        assertThatCode(() -> LocalProfileBindGuard.isLocalAndLoopback(false, "0.0.0.0")).doesNotThrowAnyException();
    }
}
