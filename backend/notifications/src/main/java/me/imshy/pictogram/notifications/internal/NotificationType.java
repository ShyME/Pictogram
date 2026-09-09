package me.imshy.pictogram.notifications.internal;

/**
 * The kind of thing that happened. The three values are exactly the
 * {@code type} discriminators carried on the {@code pictogram.social} wire
 * (ADR-0015); a fourth value on the topic is a contract break and is treated as
 * a poison record ({@link #fromWire} throws).
 */
enum NotificationType {

    POST_LIKED("post-liked"), POST_COMMENTED("post-commented"), USER_FOLLOWED("user-followed");

    private final String wireName;

    NotificationType(String wireName) {
        this.wireName = wireName;
    }

    String wireName() {
        return wireName;
    }

    static NotificationType fromWire(String wireName) {
        for (NotificationType type : values()) {
            if (type.wireName.equals(wireName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown social event type on pictogram.social: " + wireName);
    }
}
