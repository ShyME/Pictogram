package me.imshy.pictogram.identity.internal.web;

class UnusableGoogleAccountException extends RuntimeException {

    enum Reason {
        EMAIL_UNVERIFIED("email-unverified"), EMAIL_MISSING("email-missing");

        private final String slug;

        Reason(String slug) {
            this.slug = slug;
        }

        String slug() {
            return slug;
        }
    }

    private final Reason reason;

    UnusableGoogleAccountException(Reason reason, String detail) {
        super(detail);
        this.reason = reason;
    }

    Reason reason() {
        return reason;
    }
}
