package me.imshy.pictogram.shared.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class KeysetWindowTest {

    private record Row(Instant at, UUID id) {
    }

    private static final Function<Row, Cursor> CURSOR_OF = row -> new Cursor(row.at(), row.id());

    private static List<Row> rows(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> new Row(Instant.ofEpochSecond(1_700_000_000L + i), UUID.randomUUID())).toList();
    }

    @Test
    void exactlyPageSizeRowsLeavesThePageUnchangedWithNoNextCursor() {
        List<Row> fetched = rows(3);

        KeysetWindow<Row> window = KeysetWindow.of(fetched, 3, CURSOR_OF);

        assertThat(window.page()).containsExactlyElementsOf(fetched);
        assertThat(window.nextCursor()).isNull();
    }

    @Test
    void fewerThanPageSizeRowsLeavesThePageUnchangedWithNoNextCursor() {
        List<Row> fetched = rows(2);

        KeysetWindow<Row> window = KeysetWindow.of(fetched, 5, CURSOR_OF);

        assertThat(window.page()).containsExactlyElementsOf(fetched);
        assertThat(window.nextCursor()).isNull();
    }

    @Test
    void anOverFetchedRowTrimsThePageAndBuildsTheCursorFromTheLastKeptRow() {
        List<Row> fetched = rows(4);

        KeysetWindow<Row> window = KeysetWindow.of(fetched, 3, CURSOR_OF);

        assertThat(window.page()).containsExactly(fetched.get(0), fetched.get(1), fetched.get(2));
        assertThat(window.nextCursor()).isEqualTo(new Cursor(fetched.get(2).at(), fetched.get(2).id()));
    }

    @Test
    void theNextCursorResumesAfterTheLastRowTheClientSawNotTheDroppedRow() {
        List<Row> fetched = rows(10);

        KeysetWindow<Row> window = KeysetWindow.of(fetched, 4, CURSOR_OF);

        assertThat(window.nextCursor()).isEqualTo(CURSOR_OF.apply(fetched.get(3)));
        assertThat(window.nextCursor()).isNotEqualTo(CURSOR_OF.apply(fetched.get(4)));
    }

    @Test
    void anEmptyFetchIsAnEmptyLastPage() {
        KeysetWindow<Row> window = KeysetWindow.of(List.of(), 5, CURSOR_OF);

        assertThat(window.page()).isEmpty();
        assertThat(window.nextCursor()).isNull();
    }

    @Test
    void thePageDoesNotChangeWhenTheSourceListIsLaterMutated() {
        List<Row> fetched = new java.util.ArrayList<>(rows(4));

        KeysetWindow<Row> window = KeysetWindow.of(fetched, 3, CURSOR_OF);
        fetched.clear();

        assertThat(window.page()).hasSize(3);
    }

    @Test
    void rejectsAPageSizeBelowOneRatherThanIndexingOffTheEndOfAnEmptyTrim() {
        List<Row> fetched = rows(3);

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> KeysetWindow.of(fetched, 0, CURSOR_OF));
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> KeysetWindow.of(fetched, -1, CURSOR_OF));
    }

    @Test
    void limitsClampDefaultsWhenRequestedIsNull() {
        assertThat(Limits.clamp(null, 20, 50)).isEqualTo(20);
    }

    @Test
    void limitsClampRaisesBelowOneToOne() {
        assertThat(Limits.clamp(0, 20, 50)).isEqualTo(1);
        assertThat(Limits.clamp(-7, 20, 50)).isEqualTo(1);
    }

    @Test
    void limitsClampCapsAboveTheMax() {
        assertThat(Limits.clamp(1000, 20, 50)).isEqualTo(50);
    }

    @Test
    void limitsClampLeavesAnInRangeRequestUnchanged() {
        assertThat(Limits.clamp(30, 20, 50)).isEqualTo(30);
    }
}
