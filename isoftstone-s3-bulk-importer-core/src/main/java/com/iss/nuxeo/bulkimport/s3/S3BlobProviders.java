package com.iss.nuxeo.bulkimport.s3;

import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.storage.sql.S3BinaryManager;
import org.nuxeo.runtime.api.Framework;

public class S3BlobProviders {

    public static S3BinaryManager getS3BlobProvider(String id) {

        BlobManager bm = Framework.getService(BlobManager.class);
        S3BinaryManager s3bm = (S3BinaryManager)bm.getBlobProvider(id);

        return s3bm;
    }
}
