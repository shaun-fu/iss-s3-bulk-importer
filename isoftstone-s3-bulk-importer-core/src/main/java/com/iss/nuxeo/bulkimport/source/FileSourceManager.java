package com.iss.nuxeo.bulkimport.source;

import com.iss.nuxeo.bulkimport.ImportContext;

public interface FileSourceManager {

    public static final String DEFAULT_PROVIDER = "default";

    public FileSourceProvider getFileSourceProvider(String id);

    public FileSourceProvider getFileSourceProvider(ImportContext context);
}
