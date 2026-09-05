package me.imshy.pictogram.media.internal.web;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;
import me.imshy.pictogram.media.internal.MediaLibrary;
import me.imshy.pictogram.media.internal.UndecodableImageException;
import me.imshy.pictogram.shared.MediaId;
import me.imshy.pictogram.shared.UserId;
import me.imshy.pictogram.shared.http.CurrentUser;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
class MediaController {

    private final MediaLibrary library;

    MediaController(MediaLibrary library) {
        this.library = library;
    }

    record MediaUploadResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) MediaId mediaId) {
    }

    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "The image was accepted and re-encoded.",
            content = @Content(schema = @Schema(implementation = MediaUploadResponse.class))),
        @ApiResponse(responseCode = "400", description = "The upload is missing, empty, or not a readable image.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))})
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<MediaUploadResponse> upload(@CurrentUser UserId owner, @RequestParam("file") MultipartFile file) {
        MediaId mediaId = library.upload(owner, bytesOf(file));
        return ResponseEntity.created(URI.create("/api/media/" + mediaId + "/original"))
            .body(new MediaUploadResponse(mediaId));
    }

    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The full-size canonical JPEG.",
            content = @Content(mediaType = MediaType.IMAGE_JPEG_VALUE,
                schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(responseCode = "404", description = "No media has that id.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))})
    @GetMapping("/{mediaId}/original")
    ResponseEntity<byte[]> original(@PathVariable("mediaId") UUID mediaId) {
        return jpeg(library.original(new MediaId(mediaId)));
    }

    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The square thumbnail JPEG.",
            content = @Content(mediaType = MediaType.IMAGE_JPEG_VALUE,
                schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(responseCode = "404", description = "No media has that id.",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)))})
    @GetMapping("/{mediaId}/thumbnail")
    ResponseEntity<byte[]> thumbnail(@PathVariable("mediaId") UUID mediaId) {
        return jpeg(library.thumbnail(new MediaId(mediaId)));
    }

    private static ResponseEntity<byte[]> jpeg(byte[] bytes) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG)
            .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable()).body(bytes);
    }

    private static byte[] bytesOf(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException unreadable) {
            throw new UndecodableImageException();
        }
    }
}
