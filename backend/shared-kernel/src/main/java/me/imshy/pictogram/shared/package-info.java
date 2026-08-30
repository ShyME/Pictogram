/**
 * The whitelisted Modulith shared module {@code shared} (Gradle subproject
 * {@code :shared-kernel}), whitelisted via {@code @Modulithic(sharedModules = "shared")}.
 * This package holds the ID value types every bounded context references by value —
 * {@code UserId}, {@code PostId}, {@code MediaId}, {@code ViewerId}. Its sub-package
 * {@code http} carries the cross-cutting HTTP edge conventions (docs/adr/0008). Nothing
 * with domain behaviour or persistence belongs in either.
 */
package me.imshy.pictogram.shared;
