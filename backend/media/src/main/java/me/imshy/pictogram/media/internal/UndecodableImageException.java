package me.imshy.pictogram.media.internal;

import me.imshy.pictogram.shared.http.ApiException;
import me.imshy.pictogram.shared.http.ProblemType;
import org.springframework.http.HttpStatus;

public final class UndecodableImageException extends ApiException {

    static final ProblemType TYPE = new ProblemType("media-undecodable", "Upload is not a readable image");

    public UndecodableImageException() {
        super(HttpStatus.BAD_REQUEST, TYPE,
                "The upload could not be read as an image. Supported formats are JPEG, PNG and WebP.");
    }
}
