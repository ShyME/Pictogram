package me.imshy.pictogram.shared.http;

import org.springframework.http.HttpStatus;

public class OversizedBatchException extends ApiException {

    public OversizedBatchException(int max, int requested) {
        super(HttpStatus.BAD_REQUEST, ProblemType.OVERSIZED_BATCH,
                "A batch lookup takes at most %d ids; the request had %d.".formatted(max, requested));
    }
}
