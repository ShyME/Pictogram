package me.imshy.pictogram.identity;

import me.imshy.pictogram.shared.UserId;

public interface PictogramAccessTokens {

    UserId resolve(String accessToken);
}
