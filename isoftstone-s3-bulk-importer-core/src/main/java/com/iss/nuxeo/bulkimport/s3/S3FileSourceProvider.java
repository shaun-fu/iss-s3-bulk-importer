package com.iss.nuxeo.bulkimport.s3;

import com.iss.nuxeo.bulkimport.source.AbstractFileSourceProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.storage.sql.S3BinaryManager;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.iss.nuxeo.bulkimport.s3.S3Operation.S3OperationBuilder.withS3BlobProvider;
import static org.apache.commons.lang3.StringUtils.appendIfMissing;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.nuxeo.ecm.core.storage.sql.S3BinaryManager.DELIMITER;


public class S3FileSourceProvider extends AbstractFileSourceProvider {
    private static final Log log = LogFactory.getLog(S3FileSourceProvider.class);

    protected S3Operation s3;

    protected S3BlobTransfer s3BlobTransfer;
    @Override
    public void initialize(String id, Map<String, String> properties) {

        super.initialize(id, properties);

        String sourceBlobProvider = properties.get("sourceBlobProvider");
        if(isBlank(sourceBlobProvider)) {
            throw new NuxeoException(String.format("[sourceBlobProvider] is missing in S3FileSourceProvider [%s]'s properties.", id));
        }
        S3BinaryManager s3bm = S3BlobProviders.getS3BlobProvider(sourceBlobProvider);
        if(s3bm == null) {
            throw new NuxeoException(String.format("Failed to initialize S3FileSourceProvider [%s] because can't find blobprovider with id [%2s].", id, sourceBlobProvider));
        }
        s3 = withS3BlobProvider(s3bm).withBlobProviderId(sourceBlobProvider).build();

        String transientBlobProvider = properties.get("transientBlobProvider");
        if(isBlank(transientBlobProvider)) {
            log.warn(String.format("[transientBlobProvider] is missing in S3FileSourceProvider [%s]'s properties, will use [%s] as the transient blob provider.", id,sourceBlobProvider));
        }

        this.s3BlobTransfer = S3BlobTransfer.withSourceBlobProvider(sourceBlobProvider).withTransientBlobProvider(transientBlobProvider);
    }

    /**
     * Return the list of subfolders
     *
     * @param location the location would be the part of s3 key by removing the bucket prefix
     * @return the folders under the specified location, each item will be the key without bucket prefix,
     *         or just the folder name if <code>onlyName</code> is true.
     *
     */
    @Override
    public List<String> getSourceChildren(String location, boolean onlyName) {

        if(log.isDebugEnabled()) {
            log.debug(String.format("get sublocations for [%s]",location));
        }

        String prefix = this.getAbsolutePath(location, true);
        List<String> results = s3.listFolders(s3.getBucketName(), prefix);
        if (onlyName) {
            results = results.stream()
                    .map(c -> onlyName ? removeStart(c, prefix) : removeStart(c, s3.getBucketPrefix()))
                    .map(c -> removeEnd(c, DELIMITER))
                    .collect(Collectors.toList());
        }

        if(log.isDebugEnabled()) {
            log.debug(String.format("sublocations for [%s]: %s",location,results));
        }

        return results;
    }

    /**
     * Get the full s3 key name by adding the bucket prefix to the path
     * @param path relative to bucket prefix.
     */
    public String getAbsolutePath(String path, boolean isFolderish) {
        String prefix = s3.getBucketPrefix();
        if (isNotBlank(prefix)) {
            path = removeStart(path, DELIMITER);
            path = prefix + path;
        }

        path = removeStart(path, DELIMITER);

        if(isFolderish && !path.isEmpty()) {
            path = appendIfMissing(path, DELIMITER);
        }

        return path;
    }

    @Override
    public List<SourceNode> getChildrenSourceNodes(String location) throws IOException {
        // if it's not a folder
        if (!location.endsWith(DELIMITER)) {
            return null;
        }

        List<SourceNode> results = s3.listFolders(getAbsolutePath(location,true))
                .stream()
                .map(c->this.toSourceNodePath(c))
                .map(S3SourceNode::fromString)
                .collect(Collectors.toList());

        if(log.isDebugEnabled()) {
            log.debug(String.format("%s folders under %s", results.size(), location));
        }

        List<SourceNode> files = s3.listFiles(getAbsolutePath(location,true))
                .stream()
                .map(c->this.toSourceNodePath(c))
                .map(S3SourceNode::fromString)
                .collect(Collectors.toList());

        if(log.isDebugEnabled()) {
            log.debug(String.format("%s files under %s", files.size(), location));
        }

        results.addAll(files);

        return results;
    }

    @Override
    public BlobHolder getBlobHolder(String location) throws IOException {

        //represents a folder
        if(location.endsWith(DELIMITER)) {
            return new SimpleBlobHolder((Blob)null);
        }

        String name = getLastPathName(location);
        BlobHolder bh = s3BlobTransfer.transfer(location, name);

        return bh;

    }

    private String toSourceNodePath(String s3key) {
        return this.id +":" + removeStart(s3key, s3.getBucketPrefix());
    }


}
