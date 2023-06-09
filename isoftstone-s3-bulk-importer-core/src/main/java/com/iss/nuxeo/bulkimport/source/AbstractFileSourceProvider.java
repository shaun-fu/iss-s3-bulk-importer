package com.iss.nuxeo.bulkimport.source;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.nuxeo.ecm.core.storage.sql.S3BinaryManager.DELIMITER;


public abstract class AbstractFileSourceProvider implements FileSourceProvider{
    protected String id;

    protected FileSource fileset;

    @Override
    public void initialize(String id, Map<String, String> properties) {

        this.id = id;

        this.fileset = new FileSource();
        String root = properties.getOrDefault("location.root", DELIMITER);
        fileset.setRootLocation(StringUtils.appendIfMissing(root, DELIMITER));
        fileset.setCaseSensitive(properties.getOrDefault("caseSensitive", "true"));
        fileset.setExcludes(properties.get("excludes"));
        fileset.setIncludes(properties.get("includes"));
        fileset.setMetadataFileExtensions(properties.get("meta.extensions"));

    }

    @Override
    public String createSourceNodePath(String location) {
        return this.id + ":" + location;
    }

    @Override
    public String getSourceRoot() {
        return this.fileset.getRootLocation();
    }

    @Override
    public boolean isValidSublocations(String location, boolean acceptRoot) {

        if (isBlank(location)) {
            return false;
        }

        String root = this.getSourceRoot();

        if(acceptRoot && location.equals(location)) {
            return true;
        }

        return location.startsWith(root);

    }

    protected String getLastPathName(String location) {
        String path = StringUtils.removeEnd(location, DELIMITER);
        int index = path.lastIndexOf(DELIMITER);
        return path.substring(index+1);
    }

}
