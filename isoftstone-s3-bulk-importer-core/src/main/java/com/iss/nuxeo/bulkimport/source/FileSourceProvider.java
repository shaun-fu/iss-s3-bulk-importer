package com.iss.nuxeo.bulkimport.source;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface FileSourceProvider {

    public void initialize(String id, Map<String, String> properties);

    public String getSourceRoot();

    public String createSourceNodePath(String location);

    public List<String> getSourceChildren(String location, boolean onlyName);

    public boolean isValidSublocations(String location, boolean acceptRoot);

    public List<SourceNode> getChildrenSourceNodes(String location) throws IOException;

    public BlobHolder getBlobHolder(String location) throws IOException;

}
