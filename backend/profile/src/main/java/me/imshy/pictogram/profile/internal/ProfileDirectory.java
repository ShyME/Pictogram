package me.imshy.pictogram.profile.internal;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.StreamSupport;
import me.imshy.pictogram.shared.UserId;
import org.springframework.stereotype.Service;

/**
 * The read side of {@code profile}: look a public profile up by username, or resolve a set
 * of {@link UserId}s to their profiles in one query. The client composes feed cards and
 * profile pages from these (ADR-0005, no BFF), so the batch must never fan out into an
 * id-at-a-time loop; an id with no profile is simply absent from the result.
 */
@Service
public class ProfileDirectory {

    private final Profiles profiles;

    ProfileDirectory(Profiles profiles) {
        this.profiles = profiles;
    }

    public ProfileView byUsername(String username) {
        // Handles are stored lowercase (the shape rule is ^[a-z0-9_]{3,20}$), so a link
        // with different casing still resolves rather than 404ing.
        return profiles.findByUsername(username.toLowerCase(Locale.ROOT))
                .map(ProfileView::of)
                .orElseThrow(ProfileNotFoundException::new);
    }

    public List<ProfileView> byIds(Collection<UserId> ids) {
        var keys = ids.stream().map(UserId::value).toList();
        return StreamSupport.stream(profiles.findAllById(keys).spliterator(), false)
                .map(ProfileView::of)
                .toList();
    }
}
