package com.iss.nuxeo.bulkimport.s3;

import com.amazonaws.services.s3.model.ObjectMetadata;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolderWithProperties;
import org.nuxeo.ecm.core.blob.binary.Binary;
import org.nuxeo.ecm.core.blob.binary.BinaryBlob;
import org.nuxeo.ecm.core.blob.binary.LazyBinary;
import org.nuxeo.ecm.core.storage.sql.S3BinaryManager;

import java.util.Map;

import static com.iss.nuxeo.bulkimport.s3.S3Operation.S3OperationBuilder.withS3BlobProvider;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.nuxeo.ecm.core.storage.sql.S3BinaryManager.BUCKET_NAME_PROPERTY;
import static org.nuxeo.ecm.core.storage.sql.S3BinaryManager.BUCKET_PREFIX_PROPERTY;
import static org.nuxeo.ecm.core.storage.sql.S3BinaryManager.DELIMITER;

public class S3BlobTransfer {
    private String sourceBlobProvider;
    private String transientBlobProvider;
    private String targetBlobProvider;

    private S3BlobTransfer() {

    }

    public static S3BlobTransfer withSourceBlobProvider(String id) {
        S3BlobTransfer transfer = new S3BlobTransfer();
        transfer.sourceBlobProvider = id;

        return transfer;
    }

    public S3BlobTransfer withTransientBlobProvider(String id) {
        this.transientBlobProvider = id;
        return this;
    }

    public S3BlobTransfer withTargetBlobProvider(String id) {
        this.targetBlobProvider = id;
        return this;
    }

    /**
     *
     * S3 doesn't support either renaming or moving. It has to do a copy/delete pair operation.
     * To compute the file's digest and rename it to the digest and saved it to binary repository.
     * It has to copy the files across the buckets several times.
     *
     * Refer to S3BinaryManager and DocumentBlobManager to have a good understand of how Nuxeo
     * manages the blobs and it's specific requirements on the blobs.
     *
     * @param key location
     * @param name file name
     * @return
     */
    public BlobHolder transfer(String key, String name) {

        S3BinaryManager sources3 = S3BlobProviders.getS3BlobProvider(sourceBlobProvider);
        S3BinaryManager transients3 = S3BlobProviders.getS3BlobProvider(transientBlobProvider);
        if(transients3 == null) {
            transients3 = sources3;
            transientBlobProvider = sourceBlobProvider;
        }
        S3BinaryManager targets3 = S3BlobProviders.getS3BlobProvider(targetBlobProvider);
        if(targets3 == null) {
            targets3 = transients3;
            targetBlobProvider = transientBlobProvider;
        }
        S3Operation sourceOps = withS3BlobProvider(sources3).withBlobProviderId(sourceBlobProvider).build();
        S3Operation transientOps = withS3BlobProvider(transients3).withBlobProviderId(transientBlobProvider).build();
        S3Operation targetOps = withS3BlobProvider(targets3).withBlobProviderId(targetBlobProvider).build();

        String sourceKey = getS3Property(sources3, BUCKET_PREFIX_PROPERTY) + removeStart(key,DELIMITER);
        ObjectMetadata sourceMeta = sourceOps.getObjectMetadata(sourceKey, false);
        String transientKey = sourceOps.isDummyETag(sourceMeta.getETag()) ? sourceOps.getUUID() : sourceMeta.getETag();
        ObjectMetadata transientMeta = transientOps.getObjectMetadata( getS3Property(transients3, BUCKET_PREFIX_PROPERTY) + transientKey, true);
        if(transientMeta == null) {
            transientMeta = transientOps.copy(getS3Property(sources3, BUCKET_NAME_PROPERTY), sourceKey, getS3Property(transients3, BUCKET_NAME_PROPERTY), getS3Property(transients3, BUCKET_PREFIX_PROPERTY)+transientKey, null);
        }

        String targetKey = transientMeta.getETag();
        ObjectMetadata targetMeta = targetOps.getObjectMetadata(getS3Property(targets3, BUCKET_PREFIX_PROPERTY)+targetKey, true);
        if(targetMeta == null) {
            targetMeta = targetOps.copy(getS3Property(transients3, BUCKET_NAME_PROPERTY), getS3Property(transients3, BUCKET_PREFIX_PROPERTY)+transientKey, getS3Property(targets3, BUCKET_NAME_PROPERTY), getS3Property(targets3, BUCKET_PREFIX_PROPERTY)+targetKey, null);
        }

        Binary binary = new LazyBinary(targetKey, targetBlobProvider, null);
        Blob blob = new BinaryBlob(binary, targetKey, name, targetMeta.getContentType(),
                targetMeta.getContentEncoding(), targetMeta.getETag(), targetMeta.getContentLength());
        return new SimpleBlobHolderWithProperties(blob,null);

    }
    public String getS3Property(S3BinaryManager s3bm, String property) {
        Map<String, String> properties = s3bm.getProperties();
        return properties.get(property);

    }
}
