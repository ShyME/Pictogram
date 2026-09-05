package me.imshy.pictogram.shared.http;

import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class BatchIdsTest {

    @Test
    void passesASetWithinTheCapStraightThrough() {
        Set<Integer> ids = Set.of(1, 2, 3);

        assertThat(BatchIds.checked(ids)).isSameAs(ids);
    }

    @Test
    void allowsExactlyTheCap() {
        assertThat(BatchIds.checked(idsCounting(BatchIds.MAX))).hasSize(BatchIds.MAX);
    }

    @Test
    void rejectsASetOverTheCapAsABadRequest() {
        Set<Integer> tooMany = idsCounting(BatchIds.MAX + 1);

        assertThatExceptionOfType(OversizedBatchException.class).isThrownBy(() -> BatchIds.checked(tooMany))
            .satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private static Set<Integer> idsCounting(int n) {
        return IntStream.rangeClosed(1, n).boxed().collect(toUnmodifiableSet());
    }
}
