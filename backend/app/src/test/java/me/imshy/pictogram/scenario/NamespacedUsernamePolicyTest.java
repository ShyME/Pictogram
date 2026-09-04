package me.imshy.pictogram.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import me.imshy.pictogram.scenario.PictogramApi.Profile;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("fast")
class NamespacedUsernamePolicyTest {

    private final NamespacedUsernamePolicy policy = new NamespacedUsernamePolicy("bb1000");

    @Test
    void qualifyPrefixesTheUsernameWithTheNamespace() {
        assertThat(policy.qualify("ada_lovelace")).isEqualTo("bb1000ada_lovelace");
    }

    @Test
    void qualifyRejectsAUsernameThatLeavesNoRoomForTheNamespaceWithin20Chars() {
        String fitsExactly = "a".repeat(20 - "bb1000".length());
        assertThat(policy.qualify(fitsExactly)).hasSize(20);

        String oneCharTooLong = fitsExactly + "a";
        assertThatIllegalArgumentException().isThrownBy(() -> policy.qualify(oneCharTooLong));
    }

    @Test
    void stripRemovesTheNamespaceFromAQualifiedProfile() {
        Profile qualified = new Profile("u-1", "bb1000ada_lovelace", "Ada Lovelace", "Countess");

        assertThat(policy.strip(qualified)).isEqualTo(new Profile("u-1", "ada_lovelace", "Ada Lovelace", "Countess"));
    }

    @Test
    void stripLeavesAProfileWithoutTheNamespaceUnchanged() {
        Profile unqualified = new Profile("u-1", "ada_lovelace", "Ada Lovelace", "Countess");

        assertThat(policy.strip(unqualified)).isSameAs(unqualified);
    }
}
