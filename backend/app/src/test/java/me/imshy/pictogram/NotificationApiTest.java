package me.imshy.pictogram;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import me.imshy.pictogram.shared.http.ProblemType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

/**
 * The HTTP edge of the {@code notifications} read side (#198): the three
 * endpoints all need a viewer, the list is the feed's keyset envelope newest
 * first, a row carries {@code actorId} only, and {@code mark-read} is an
 * idempotent 204. Rows are seeded straight into the table — the Kafka ingest
 * path is {@code notifications}' own suite; this is only the edge.
 */
@AppIntegrationTest
class NotificationApiTest {

    private static final Instant WHEN = Instant.parse("2026-09-09T12:00:00Z");

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcClient db;

    @Test
    void everyEndpointNeedsAToken() throws Exception {
        for (RequestBuilder request : new RequestBuilder[]{get("/api/notifications"),
            get("/api/notifications/unread-count"), post("/api/notifications/mark-read")}) {
            mvc.perform(request).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value(ProblemType.UNAUTHORIZED.uri().toString()));
        }
    }

    @Test
    void theListIsTheKeysetEnvelopeNewestFirstCarryingActorIdOnly() throws Exception {
        UUID recipient = UUID.randomUUID();
        UUID liker = UUID.randomUUID();
        UUID post = UUID.randomUUID();
        UUID follower = UUID.randomUUID();
        givenNotification(recipient, follower, "user-followed", null, WHEN, WHEN.plusSeconds(1), false);
        givenNotification(recipient, liker, "post-liked", post, WHEN, WHEN.plusSeconds(2), false);
        givenNotification(UUID.randomUUID(), liker, "post-liked", post, WHEN, WHEN.plusSeconds(3), false);

        mvc.perform(get("/api/notifications").with(jwt().jwt(jwt -> jwt.subject(recipient.toString()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.nextCursor").doesNotExist())
            .andExpect(jsonPath("$.items[0].type").value("post-liked"))
            .andExpect(jsonPath("$.items[0].actorId").value(liker.toString()))
            .andExpect(jsonPath("$.items[0].subjectPostId").value(post.toString()))
            .andExpect(jsonPath("$.items[0].occurredAt").value("2026-09-09T12:00:00Z"))
            .andExpect(jsonPath("$.items[0].read").value(false))
            .andExpect(jsonPath("$.items[0].actorUsername").doesNotExist())
            .andExpect(jsonPath("$.items[0].actor").doesNotExist())
            .andExpect(jsonPath("$.items[1].type").value("user-followed"))
            .andExpect(jsonPath("$.items[1].subjectPostId").value(nullValue()));
    }

    @Test
    void theListKeysetPagesNewestFirst() throws Exception {
        UUID recipient = UUID.randomUUID();
        for (int i = 0; i < 3; i++) {
            givenNotification(recipient, UUID.randomUUID(), "post-liked", UUID.randomUUID(), WHEN, WHEN.plusSeconds(i),
                false);
        }

        String cursor = com.jayway.jsonpath.JsonPath.read(
            mvc.perform(
                get("/api/notifications").param("limit", "2").with(jwt().jwt(jwt -> jwt.subject(recipient.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty()).andReturn().getResponse().getContentAsString(),
            "$.nextCursor");

        mvc.perform(get("/api/notifications").param("cursor", cursor).param("limit", "2")
            .with(jwt().jwt(jwt -> jwt.subject(recipient.toString())))).andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void aMalformedCursorIsRejected() throws Exception {
        mvc.perform(get("/api/notifications").param("cursor", "not-a-cursor")
            .with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString())))).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value(ProblemType.INVALID_CURSOR.uri().toString()));
    }

    @Test
    void theUnreadCountReturnsOnlyTheCallersUnread() throws Exception {
        UUID recipient = UUID.randomUUID();
        givenNotification(recipient, UUID.randomUUID(), "post-liked", UUID.randomUUID(), WHEN, WHEN, false);
        givenNotification(recipient, UUID.randomUUID(), "post-liked", UUID.randomUUID(), WHEN, WHEN.plusSeconds(1),
            true);
        givenNotification(UUID.randomUUID(), UUID.randomUUID(), "post-liked", UUID.randomUUID(), WHEN, WHEN, false);

        mvc.perform(get("/api/notifications/unread-count").with(jwt().jwt(jwt -> jwt.subject(recipient.toString()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void markReadClearsTheCountAndIsIdempotent() throws Exception {
        UUID recipient = UUID.randomUUID();
        for (int i = 0; i < 3; i++) {
            givenNotification(recipient, UUID.randomUUID(), "post-liked", UUID.randomUUID(), WHEN, WHEN.plusSeconds(i),
                false);
        }

        mvc.perform(post("/api/notifications/mark-read").with(jwt().jwt(jwt -> jwt.subject(recipient.toString()))))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/notifications/unread-count").with(jwt().jwt(jwt -> jwt.subject(recipient.toString()))))
            .andExpect(jsonPath("$.count").value(0));

        mvc.perform(post("/api/notifications/mark-read").with(jwt().jwt(jwt -> jwt.subject(recipient.toString()))))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/notifications/unread-count").with(jwt().jwt(jwt -> jwt.subject(recipient.toString()))))
            .andExpect(jsonPath("$.count").value(0));
    }

    private void givenNotification(UUID recipient, UUID actor, String type, UUID subject, Instant occurredAt,
        Instant createdAt, boolean read) {
        db.sql("""
            insert into notification.notification
                (id, type, recipient_id, actor_id, subject_id, occurred_at, read, created_at)
            values (gen_random_uuid(), ?, cast(? as uuid), cast(? as uuid), cast(? as uuid), ?, ?, ?)
            """).params(type, recipient.toString(), actor.toString(), subject == null ? null : subject.toString(),
            Timestamp.from(occurredAt), read, Timestamp.from(createdAt)).update();
    }
}
