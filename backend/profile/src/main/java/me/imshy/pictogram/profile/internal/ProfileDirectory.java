package me.imshy.pictogram.profile.internal;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.StreamSupport;
import me.imshy.pictogram.shared.UserId;
import org.springframework.stereotype.Service;

@Service
public class ProfileDirectory {

    private final Profiles profiles;

    ProfileDirectory(Profiles profiles) {
        this.profiles = profiles;
    }

    public ProfileView byUsername(String username) {
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
