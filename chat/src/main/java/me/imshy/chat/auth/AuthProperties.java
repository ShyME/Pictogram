package me.imshy.chat.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("pictogram.auth")
public record AuthProperties(String publicKey, @DefaultValue("pictogram") String issuer) {
}
