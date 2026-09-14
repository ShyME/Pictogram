package me.imshy.pictogram.media.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties("pictogram.media.quota")
record MediaQuotaProperties(@DefaultValue("100MB") DataSize maxBytesPerUser) {
}
