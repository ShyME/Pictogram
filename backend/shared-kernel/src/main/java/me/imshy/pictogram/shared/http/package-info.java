/**
 * The HTTP edge conventions every bounded context's {@code internal.web} package reuses:
 * RFC 9457 Problem Details, the {@code ApiPage} pagination envelope with its opaque
 * {@link me.imshy.pictogram.shared.http.Cursor}, and {@code @CurrentUser} resolution of
 * the access token into a {@code UserId} / {@code ViewerId} (spec §API, docs/adr/0008).
 * A named interface of the whitelisted {@code shared} module.
 */
@org.springframework.modulith.NamedInterface("http")
package me.imshy.pictogram.shared.http;
