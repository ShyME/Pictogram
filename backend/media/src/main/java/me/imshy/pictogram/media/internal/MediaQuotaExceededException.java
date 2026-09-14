package me.imshy.pictogram.media.internal;

import me.imshy.pictogram.shared.http.ApiException;
import me.imshy.pictogram.shared.http.ProblemType;
import org.springframework.http.HttpStatus;

public final class MediaQuotaExceededException extends ApiException {

    static final ProblemType TYPE = new ProblemType("media-quota-exceeded", "Storage quota exceeded");

    public MediaQuotaExceededException() {
        super(HttpStatus.PAYLOAD_TOO_LARGE, TYPE,
            "This upload would push your total stored media past your storage quota.");
    }
}
