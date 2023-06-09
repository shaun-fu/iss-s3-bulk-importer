package com.iss.nuxeo.bulkimport.jaxrs;

import org.nuxeo.ecm.webengine.app.WebEngineModule;

/**
 * Define a WebEngine Application based on Default bulk importer JAR-RS interface
 */
public class BulkImportWebApp extends WebEngineModule {
    @Override
    public Class<?>[] getWebTypes() {
        return new Class<?>[] {BulkImportWebRoot.class, BulkImportSource.class, BulkImportTask.class,};
    }
}
