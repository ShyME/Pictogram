package me.imshy.pictogram.shared.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApiPageTest {

    @Test
    void carriesTheEncodedCursorWhenThereIsAnotherPage() {
        var next = new Cursor(Instant.parse("2026-08-30T12:34:56Z"), UUID.randomUUID());

        var page = ApiPage.of(List.of("a", "b"), next);

        assertThat(page.items()).containsExactly("a", "b");
        assertThat(page.nextCursor()).isEqualTo(next.encode());
    }

    @Test
    void hasANullCursorOnTheLastPage() {
        var page = ApiPage.of(List.of("only"), null);

        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void copiesItsItemsSoLaterMutationOfTheSourceDoesNotLeakIn() {
        var source = new ArrayList<>(List.of("a"));

        var page = ApiPage.lastPage(source);
        source.add("b");

        assertThat(page.items()).containsExactly("a");
    }

    @Test
    void isAnUnmodifiableView() {
        var page = ApiPage.lastPage(List.of("a"));

        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> page.items().add("b"));
    }
}
