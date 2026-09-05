package me.imshy.pictogram.scenario;

import me.imshy.pictogram.scenario.PictogramApp.Profile;

interface UsernameStrategy {

    String qualify(String username);

    Profile strip(Profile profile);
}
