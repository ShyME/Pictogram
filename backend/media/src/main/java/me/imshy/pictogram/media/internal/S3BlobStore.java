package me.imshy.pictogram.media.internal;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * {@link BlobStore} over the S3 API. The bucket is created on first write rather than at
 * startup, so a fresh MinIO (local, CI, a new environment) needs no provisioning step and
 * an app boot never depends on the object store being reachable.
 */
@Component
class S3BlobStore implements BlobStore {

    private final S3Client s3;
    private final String bucket;
    private volatile boolean bucketReady;

    S3BlobStore(S3Client mediaS3Client, MediaStorageProperties storage) {
        this.s3 = mediaS3Client;
        this.bucket = storage.bucket();
    }

    @Override
    public void put(String key, byte[] bytes) {
        ensureBucket();
        s3.putObject(request -> request.bucket(bucket).key(key).contentType(MediaType.IMAGE_JPEG_VALUE),
                RequestBody.fromBytes(bytes));
    }

    @Override
    public byte[] get(String key) {
        return s3.getObjectAsBytes(request -> request.bucket(bucket).key(key)).asByteArray();
    }

    @Override
    public void remove(String key) {
        // S3 (and MinIO) answer a delete of an absent key with success, so the sweep needs
        // no existence check and a half-finished previous sweep re-runs cleanly.
        s3.deleteObject(request -> request.bucket(bucket).key(key));
    }

    private void ensureBucket() {
        if (bucketReady) {
            return;
        }
        synchronized (this) {
            if (bucketReady) {
                return;
            }
            if (!bucketExists()) {
                try {
                    s3.createBucket(request -> request.bucket(bucket));
                } catch (BucketAlreadyOwnedByYouException | BucketAlreadyExistsException raced) {
                    // another instance got there first — fine
                }
            }
            bucketReady = true;
        }
    }

    private boolean bucketExists() {
        try {
            s3.headBucket(request -> request.bucket(bucket));
            return true;
        } catch (NoSuchBucketException absent) {
            return false;
        } catch (S3Exception e) {
            // MinIO answers a missing bucket with a bare 404/403 rather than the typed
            // error; anything else (auth, connectivity) is a real fault.
            if (e.statusCode() == 404 || e.statusCode() == 403) {
                return false;
            }
            throw e;
        }
    }
}
