package me.imshy.pictogram.media.internal;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
class S3BlobStore implements BlobStore {

    private final S3Client s3Client;
    private final String bucket;
    private volatile boolean isBucketReady;

    S3BlobStore(S3Client mediaS3Client, MediaStorageProperties storage) {
        this.s3Client = mediaS3Client;
        this.bucket = storage.bucket();
    }

    @Override
    public void put(String key, byte[] bytes) {
        ensureBucket();
        s3Client.putObject(request -> request.bucket(bucket).key(key).contentType(MediaType.IMAGE_JPEG_VALUE),
            RequestBody.fromBytes(bytes));
    }

    @Override
    public byte[] get(String key) {
        return s3Client.getObjectAsBytes(request -> request.bucket(bucket).key(key)).asByteArray();
    }

    @Override
    public void remove(String key) {
        s3Client.deleteObject(request -> request.bucket(bucket).key(key));
    }

    // Dev convenience only. A real deployment pre-creates the bucket and drops
    // s3:CreateBucket
    // from this role, so in prod the branch below never runs (ADR-0011 / SEC-8).
    private void ensureBucket() {
        if (isBucketReady) {
            return;
        }
        synchronized (this) {
            if (isBucketReady) {
                return;
            }
            if (!bucketExists()) {
                try {
                    s3Client.createBucket(request -> request.bucket(bucket));
                } catch (BucketAlreadyOwnedByYouException | BucketAlreadyExistsException raced) {
                }
            }
            isBucketReady = true;
        }
    }

    private boolean bucketExists() {
        try {
            s3Client.headBucket(request -> request.bucket(bucket));
            return true;
        } catch (NoSuchBucketException absent) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404 || e.statusCode() == 403) {
                return false;
            }
            throw e;
        }
    }
}
