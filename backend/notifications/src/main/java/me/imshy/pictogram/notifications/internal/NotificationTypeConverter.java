package me.imshy.pictogram.notifications.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link NotificationType} as its wire name, matching the topic
 * contract and the SQL default.
 */
@Converter(autoApply = true)
class NotificationTypeConverter implements AttributeConverter<NotificationType, String> {

    @Override
    public String convertToDatabaseColumn(NotificationType type) {
        return type == null ? null : type.wireName();
    }

    @Override
    public NotificationType convertToEntityAttribute(String wireName) {
        return wireName == null ? null : NotificationType.fromWire(wireName);
    }
}
