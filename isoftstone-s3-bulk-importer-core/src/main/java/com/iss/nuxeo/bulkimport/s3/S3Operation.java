package com.iss.nuxeo.bulkimport.s3;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.storage.sql.S3BinaryManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.iss.nuxeo.bulkimport.common.StringTool.parseByteSize;
import static com.iss.nuxeo.bulkimport.s3.S3OperationTrace.*;
import static com.iss.nuxeo.bulkimport.s3.S3OperationTrace.computeMD5;
import static com.iss.nuxeo.bulkimport.s3.S3OperationTrace.copy;
import static com.iss.nuxeo.bulkimport.s3.S3OperationTrace.move;
import static com.iss.nuxeo.bulkimport.s3.S3OperationTrace.rename;
import static com.iss.nuxeo.bulkimport.s3.S3OperationTrace.renameToDigest;
import static org.nuxeo.ecm.core.storage.sql.S3BinaryManager.BUCKET_NAME_PROPERTY;
import static org.nuxeo.ecm.core.storage.sql.S3BinaryManager.BUCKET_PREFIX_PROPERTY;
import static org.nuxeo.ecm.core.storage.sql.S3BinaryManager.DELIMITER;
import static org.nuxeo.ecm.core.storage.sql.S3Utils.NON_MULTIPART_COPY_MAX_SIZE;


public class S3Operation {

    private static final Log log = LogFactory.getLog(S3BinaryManager.class);

    public static final String TRACE_LEVEL_PROPERTY = "trace.level";

    public static final String SINGLE_PART_MAXSIZE_PROPERTY = "singlepart.maxsize";

    protected long singlePartMaxSize = NON_MULTIPART_COPY_MAX_SIZE;

    protected AmazonS3 amazonS3;

    protected String bucketName;

    protected String bucketPrefix = "";

    protected String dummyETag = "00000000000000000000000000000000-1";

    protected TraceLevel traceLevel;

    protected String blobProviderId;

    private S3Operation() {

    }

    public ObjectMetadata copy(String sourceBucket, String sourceKey, String targetBucket, String targetKey,
                               String targetSSEAlgorithm) {

        return this.copy(sourceBucket, sourceKey, targetBucket, targetKey, targetSSEAlgorithm,true);

    }

    private ObjectMetadata copy(String sourceBucket, String sourceKey, String targetBucket, String targetKey,
                                String targetSSEAlgorithm, boolean trace) {

        ObjectMetadata sourceMetadata = amazonS3.getObjectMetadata(sourceBucket, sourceKey);

        S3OperationTrace s3trace = null;
        if(trace) {
            s3trace = S3OperationTrace.get(log, null, traceLevel);
            s3trace.stepBegins(copy, sourceBucket, sourceKey, sourceMetadata, targetBucket, targetKey);
        }

        long length = sourceMetadata.getContentLength();
        ObjectMetadata newMetadata;
        if (length > this.singlePartMaxSize) {
            newMetadata = IssS3Utils.copyFile(amazonS3, sourceMetadata, sourceBucket, sourceKey, targetBucket, targetKey,
                    targetSSEAlgorithm, false);
        } else {
            newMetadata = IssS3Utils.copyFileNonMultipart(amazonS3, sourceMetadata, sourceBucket, sourceKey, targetBucket,
                    targetKey, targetSSEAlgorithm, false);
        }

        if(trace) {
            s3trace.stepEnds(copy, sourceBucket, sourceKey, targetBucket, targetKey, newMetadata);
        }

        return newMetadata;

    }

    public ObjectMetadata move(String sourceBucket, String sourceKey, String targetBucket, String targetKey,
                               String targetSSEAlgorithm ) {

        return this.move(sourceBucket, sourceKey, targetBucket, targetKey, targetSSEAlgorithm, true);

    }

    private ObjectMetadata move(String sourceBucket, String sourceKey, String targetBucket, String targetKey,
                                String targetSSEAlgorithm, boolean trace) {

        ObjectMetadata sourceMetadata = amazonS3.getObjectMetadata(sourceBucket, sourceKey);

        S3OperationTrace s3trace = null;
        if(trace) {
            s3trace = S3OperationTrace.get(log, null, traceLevel);
            s3trace.stepBegins(move, sourceBucket, sourceKey, sourceMetadata, targetBucket, targetKey);
        }

        long length = sourceMetadata.getContentLength();

        ObjectMetadata newMetadata;
        if (length > this.singlePartMaxSize) {
            newMetadata = IssS3Utils.copyFile(amazonS3, sourceMetadata, sourceBucket, sourceKey, targetBucket, targetKey, targetSSEAlgorithm, true);
        } else {
            newMetadata = IssS3Utils.copyFileNonMultipart(amazonS3, sourceMetadata, sourceBucket, sourceKey, targetBucket, targetKey, targetSSEAlgorithm, true);
        }

        if(trace) {
            s3trace.stepEnds(move, sourceBucket, sourceKey, targetBucket, targetKey, newMetadata);
        }

        return newMetadata;

    }

    public ObjectMetadata renameToDigest(final String key, final String sseAlgorithm) {

        S3OperationTrace trace = S3OperationTrace.get(log, renameToDigest, traceLevel);

        trace.operationBegins(bucketName, key);

        String objectName = getObjectName(key);

        ObjectMetadata metadata = null;
        try {
            metadata = amazonS3.getObjectMetadata(bucketName, key);
        } catch (AmazonServiceException e) {
            if(e.getStatusCode() == 404) {
                log.error(String.format("not find s3 object with key {%s} on bucket {%s}, blobprovider [%s]", key, bucketName, blobProviderId),e);
            }
            throw e;
        }

        String etag = metadata.getETag();

        if (!this.isMultipartDigest(etag) && etag.equals(objectName)) {
            trace.operationEnds(bucketName, key, bucketName, key, metadata);
            return metadata;
        }

        String newKey = dummyETag.equals(etag) ? getUUID() : etag;
        String finalKey = StringUtils.isBlank(bucketPrefix) ? newKey : bucketPrefix + newKey;

        String step = isMultipartDigest(etag) ? computeMD5 : renameToDigest;
        trace.stepBegins(step, bucketName, key, metadata, bucketName, finalKey);
        metadata = copy(bucketName, key, bucketName, finalKey, sseAlgorithm, false);
        trace.stepEnds(step, bucketName, key, bucketName, finalKey, metadata);

        String finalETag = metadata.getETag();

        if (!etag.equals(finalETag)) {
            step = rename;
            String previousKey = finalKey;
            finalKey = StringUtils.isBlank(bucketPrefix) ? finalETag : bucketPrefix + finalETag;
            trace.stepBegins(step, bucketName, previousKey, metadata, bucketName, finalKey);
            metadata = move(bucketName, previousKey, bucketName, finalKey, sseAlgorithm, false);
            trace.stepEnds(step, bucketName, previousKey, bucketName, finalKey, metadata);
        }

        trace.operationEnds(bucketName, key, bucketName, finalKey, metadata);

        return metadata;
    }

    public List<String> listFolders(String bucket, String prefix) {
        ListObjectsRequest req = new ListObjectsRequest()
                .withBucketName(bucket)
                .withDelimiter(DELIMITER)
                .withPrefix(prefix);
        ObjectListing list = amazonS3.listObjects(req);
        List<String> prefixes = list.getCommonPrefixes();
        return prefixes;
    }

    public List<String> listFolders(String prefix) {
        return this.listFolders(bucketName, prefix);
    }

    public List<String> listFiles(String bucket, String prefix) {
        ListObjectsRequest req = new ListObjectsRequest()
                .withBucketName(bucket)
                .withDelimiter(DELIMITER)
                .withPrefix(prefix);
        ObjectListing list = amazonS3.listObjects(req);
        List<String> result = list.getObjectSummaries().stream()
                .map(o -> o.getKey())
                .collect(Collectors.toList());
        while (list.isTruncated()) {
            list = amazonS3.listNextBatchOfObjects(list);
            result.addAll(list.getObjectSummaries().stream()
                    .map(o -> o.getKey())
                    .collect(Collectors.toList()));
        }
        result.remove(prefix);
        return result;
    }

    public List<String> listFiles(String prefix) {
        return this.listFiles(bucketName, prefix);
    }

    /**
     * Return object metadata if it exists, otherwise return null.
     * TODO: check null in the caller.
     *
     * @param key
     * @return
     */
    public ObjectMetadata getObjectMetadata(String key, boolean supress404) {

        try {
            return amazonS3.getObjectMetadata(bucketName, key);
        } catch (AmazonServiceException e) {
            if(supress404 && isMissingKey(e)) {
                return null;
            }
            log.error(String.format("failed to find s3 object with key {%s} on bucket {%s}, blobprovider [%s]", key, bucketName, blobProviderId),e);
            throw e;
        }

    }

    private boolean isMissingKey(AmazonServiceException e) {
        return (e.getStatusCode() == 404) || "NoSuchKey".equals(e.getErrorCode())
                || "Not Found".equals(e.getMessage());
    }

    public AmazonS3 getAmazonS3() {
        return amazonS3;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getBucketPrefix() {
        return bucketPrefix;
    }

    public String getUUID() {
        return "BI-" + UUID.randomUUID();
    }

    public boolean isMultipartDigest(String etag) {

        return etag.indexOf('-') != -1;
    }

    public boolean isDummyETag(String etag) {
        return this.dummyETag.equals(etag);
    }

    private String getObjectName(String key) {

        if (key == null) {
            return null;
        }

        int pos = key.lastIndexOf(DELIMITER);
        return key.substring(pos + 1, key.length());
        // should just strip bucket prefix??
    }

    public static class S3OperationBuilder {

        private S3Operation operation;

        public static S3OperationBuilder withAmazonS3(AmazonS3 amazonS3) {

            S3OperationBuilder builder = new S3OperationBuilder();
            builder.operation = new S3Operation();
            builder.operation.amazonS3 = amazonS3;

            return builder;
        }

        public static S3OperationBuilder withS3BlobProvider(S3BinaryManager blobProvider) {

            S3OperationBuilder builder = withAmazonS3(blobProvider.getAmazonS3())
                    .withProperties(blobProvider.getProperties());

            return builder;

        }

        public S3OperationBuilder withProperties(Map<String, String> properties) {

            if(log.isDebugEnabled()) {
                log.debug("initialize with properties: " + properties);
            }

            if(properties != null) {

                operation.bucketName = properties.get(BUCKET_NAME_PROPERTY);
                operation.bucketPrefix = StringUtils.defaultString(properties.get(BUCKET_PREFIX_PROPERTY),operation.bucketPrefix);

                operation.singlePartMaxSize = parseByteSize(properties.get(SINGLE_PART_MAXSIZE_PROPERTY), NON_MULTIPART_COPY_MAX_SIZE);

                operation.traceLevel = TraceLevel.fromString(properties.get(TRACE_LEVEL_PROPERTY));

            }

            return this;
        }

        public S3OperationBuilder withBucketName(String name) {

            operation.bucketName = name;

            return this;
        }

        public S3OperationBuilder withBucketPrefix(String prefix) {

            operation.bucketPrefix = prefix;

            return this;
        }

        public S3OperationBuilder withTraceLevel(String level) {

            operation.traceLevel = TraceLevel.fromString(level);

            return this;
        }

        public S3OperationBuilder withSinglePartMaxSize(String maxSize) {

            operation.singlePartMaxSize = parseByteSize(maxSize, NON_MULTIPART_COPY_MAX_SIZE);

            return this;
        }

        public S3OperationBuilder withBlobProviderId(String id) {

            operation.blobProviderId = id;

            return this;
        }

        public S3Operation build() {
            return operation;
        }
    }

}

