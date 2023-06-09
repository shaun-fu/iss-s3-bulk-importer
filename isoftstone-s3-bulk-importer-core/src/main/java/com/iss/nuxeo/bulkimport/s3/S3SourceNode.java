package com.iss.nuxeo.bulkimport.s3;

import com.iss.nuxeo.bulkimport.source.FileSourceManager;
import com.iss.nuxeo.bulkimport.source.FileSourceProvider;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.importer.source.FileSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.runtime.api.Framework;

import java.io.IOException;
import java.util.List;

import static com.iss.nuxeo.bulkimport.source.FileSourceManager.DEFAULT_PROVIDER;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.nuxeo.ecm.core.storage.sql.S3BinaryManager.DELIMITER;

/**
 * Override Nuxeo bulk import SourceNode
 * iterate the files on S3 transient bucket and create binaries to be imported
 */
public class S3SourceNode extends FileSourceNode implements SourceNode {
    private String fileSetProviderId;
    private String location;
    private String name;
    private boolean isFolderish;

    private BlobHolder blobHolder;

    public static S3SourceNode fromString(String sourcePath) {
        return new S3SourceNode(sourcePath);
    }

    /**
     *
     * @param sourcePath use pattern "source:path"
     *                    where: source is the fileSetProviderId
     *                          path is the location relative to bucket prefix, and it must
     *                                    end with "/" for a virtual folder
     */
    public S3SourceNode(String sourcePath) {

        super(sourcePath);

        String[] items = sourcePath.split(":") ;
        if(items.length!=1 && items.length!=2) {
            throw new NuxeoException("not a valid source path "+sourcePath);
        }

        this.fileSetProviderId = (items.length == 1) ? DEFAULT_PROVIDER : items[0];
        this.location = (items.length == 1) ? items[0] : items[1];

        this.name = getLastPathName(location);
        this.isFolderish = location.endsWith(DELIMITER);
    }

    @Override
    public boolean isFolderish() {
        return isFolderish;
    }

    @Override
    public BlobHolder getBlobHolder() throws IOException {
        if(blobHolder == null) {
            blobHolder = getFileServiceProvider().getBlobHolder(location);
        }
        return blobHolder;
    }

    @Override
    public List<SourceNode> getChildren() throws IOException {

        return getFileServiceProvider().getChildrenSourceNodes(location);

    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSourcePath() {
        return location;
    }

    private FileSourceProvider getFileServiceProvider() {
        FileSourceManager fpm =  Framework.getService(FileSourceManager.class);
        if(fpm == null) {
            throw new NuxeoException("Component for FileSourceManager class is not deployed.");
        }
        FileSourceProvider provider = fpm.getFileSourceProvider(fileSetProviderId);
        if(provider == null) {
            throw new NuxeoException("The FileSourceProvider [" + fileSetProviderId + "] doesn't exist.");
        } else {
            return provider;
        }
    }

    private String getLastPathName(String location) {
        String path = removeEnd(location, DELIMITER);
        int index = path.lastIndexOf(DELIMITER);
        return path.substring(index+1);
    }
}
