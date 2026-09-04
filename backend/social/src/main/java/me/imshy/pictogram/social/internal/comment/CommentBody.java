package me.imshy.pictogram.social.internal.comment;

record CommentBody(String value) {

    static final int MAX_LENGTH = 1000;

    static CommentBody of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new EmptyCommentException();
        }
        String trimmed = raw.strip();
        if (trimmed.codePointCount(0, trimmed.length()) > MAX_LENGTH) {
            throw new CommentTooLongException();
        }
        return new CommentBody(trimmed);
    }
}
