package me.imshy.pictogram.profile.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

interface Profiles extends CrudRepository<Profile, UUID> {

    boolean existsByUsername(String username);

    Optional<Profile> findByUsername(String username);
}
