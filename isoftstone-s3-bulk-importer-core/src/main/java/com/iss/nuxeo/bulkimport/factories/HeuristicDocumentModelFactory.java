package com.iss.nuxeo.bulkimport.factories;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.platform.importer.factories.DefaultDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.ecm.core.api.Blob;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * Search the following precedences to determine the folder doctype.
 *   1. the doc type specified in a metadata properties file.
 *   2. the folderish doctype configured in the bulk import.
 *   3. the allowed child types of the parent node's doctype.
 *   4. the default doctype
 */

public class HeuristicDocumentModelFactory extends DefaultDocumentModelFactory {
    private static final Log log = LogFactory.getLog(HeuristicDocumentModelFactory.class);

    protected static final String DEFAULT_FOLDERISH_DOCTYPE = "Folder";
    protected static final String DEFAULT_LEAF_DOCTYPE = "File";

    public static final String ISS_BULK_IMPORT_FOLDER_DOC_TYPE = "iss.bulk.import.folder.type";
    public static final String ISS_BULK_IMPORT_LEAF_DOC_TYPE = "iss.bulk.import.leaf.type";


    public HeuristicDocumentModelFactory() {
        this(DEFAULT_FOLDERISH_DOCTYPE, DEFAULT_LEAF_DOCTYPE);
    }

    public HeuristicDocumentModelFactory(String folderishType, String leafType) {
        super(folderishType, leafType);
    }

    @Override
    public DocumentModel createFolderishNode(CoreSession session, DocumentModel parent, SourceNode node)
            throws IOException {

        String name = getValidNameFromFileName(node.getName());

        BlobHolder bh = node.getBlobHolder();
        String folderishTypeToUse = deduceFolderDocType(parent, bh);

        List<String> facets = getFacetsToUse(bh);

        DocumentModel doc = session.createDocumentModel(parent.getPathAsString(), name, folderishTypeToUse);
        for (String facet : facets) {
            doc.addFacet(facet);
        }
        this.enrichMetadata(doc, session);
        doc = session.createDocument(doc);
        if (bh != null) {
            doc = setDocumentProperties(session, bh.getProperties(), doc);
        }

        return doc;
    }

    public String deduceFolderDocType(DocumentModel parent, BlobHolder bh) throws IOException{

        String docType = getDocTypeToUse(bh);

        if(!StringUtils.isBlank(docType)) {
            return docType;
        }

        SchemaManager schema = Framework.getService(SchemaManager.class);
        DocumentType dt = schema.getDocumentType(parent.getType());
        Collection<String> dtc = dt.getAllowedSubtypes();
        List<String> dtl = dtc.stream()
                .map(schema::getDocumentType)
                .filter(s->s.isFolder())
                .map(d->d.getName())
                .collect(Collectors.toCollection(ArrayList::new));;

        String preferredFolderDocType = Framework.getProperty(ISS_BULK_IMPORT_FOLDER_DOC_TYPE);
        if (StringUtils.isBlank(preferredFolderDocType)){
            preferredFolderDocType = StringUtils.isBlank(folderishType) ? DEFAULT_FOLDERISH_DOCTYPE : folderishType;
        }

        if(dtl.isEmpty()) {
            log.warn(String.format("use default doctype %1$s, there is no allowed folderish child types under %2$s, ", preferredFolderDocType, dt.getName()));
            return preferredFolderDocType;
        }
        else if(dtl.contains(preferredFolderDocType)){
            log.info(String.format("use configured doctype %1$s which is an allowed folderish child types under %2$s.", preferredFolderDocType, dt.getName()));
            return preferredFolderDocType;
        }
        else {
            log.info(String.format("pick up %1$s as the folderish doctype from %2$s's allowed folderish child types %3$s.", dtl.get(0), dt.getName(), dtl));
            return dtl.get(0);
        }

    }

    @Override
    public DocumentModel defaultCreateLeafNode(CoreSession session, DocumentModel parent, SourceNode node)
            throws IOException {

        Blob blob = null;
        Map<String, Serializable> props = null;
//        String leafTypeToUse = leafType;
        BlobHolder bh = node.getBlobHolder();
        String leafTypeToUse = "";

        if (bh != null) {
            blob = bh.getBlob();
            props = bh.getProperties();
            String bhType = getDocTypeToUse(bh);
            if (bhType != null) {
                leafTypeToUse = bhType;
            }
        }

        if (StringUtils.isBlank(leafTypeToUse)){
            leafTypeToUse = deduceLeafDocType(parent);
        }

        String fileName = node.getName();
        String name = getValidNameFromFileName(fileName);
        DocumentModel doc = session.createDocumentModel(parent.getPathAsString(), name, leafTypeToUse);
        for (String facet : getFacetsToUse(bh)) {
            doc.addFacet(facet);
        }
        doc.setProperty("dublincore", "title", node.getName());
        if (blob != null && blob.getLength() > 0) {
            blob.setFilename(fileName);
            doc.setProperty("file", "content", blob);
        }
        doc = session.createDocument(doc);
        if (props != null) {
            doc = setDocumentProperties(session, props, doc);
        }
        return doc;
    }

    public String deduceLeafDocType(DocumentModel parent){
        SchemaManager schema = Framework.getService(SchemaManager.class);
        DocumentType dt = schema.getDocumentType(parent.getType());
        Collection<String> dtc = dt.getAllowedSubtypes();
        List<String> dtl = dtc.stream()
                .map(schema::getDocumentType)
                .map(d->d.getName())
                .collect(Collectors.toCollection(ArrayList::new));

        String preferredFileDocType = Framework.getProperty(ISS_BULK_IMPORT_LEAF_DOC_TYPE);
        if (StringUtils.isBlank(preferredFileDocType)){
            preferredFileDocType = StringUtils.isBlank(leafType) ? DEFAULT_LEAF_DOCTYPE : leafType;
        }

        if(dtl.isEmpty()) {
            log.warn(String.format("use default doctype %1$s, there is no allowed file child types under %2$s, ", preferredFileDocType, dt.getName()));
            return preferredFileDocType;
        }
        else if(dtl.contains(preferredFileDocType)){
            log.info(String.format("use configured doctype %1$s which is an allowed file child types under %2$s.", preferredFileDocType, dt.getName()));
            return preferredFileDocType;
        }
        else {
            log.info(String.format("pick up %1$s as the file doctype from %2$s's allowed file child types %3$s.", dtl.get(0), dt.getName(), dtl));
            return dtc.stream()
                    .map(schema::getDocumentType).filter(s->s.isFile())
                    .map(d->d.getName()).findFirst().get();
        }
    }

    public void enrichMetadata(DocumentModel doc, CoreSession session) {
        doc.setProperty("dublincore", "title", doc.getName());
        NuxeoPrincipal p =  NuxeoPrincipal.getCurrent();
        if(p!=null) {
            doc.setProperty("dublincore", "creator", p.getName());
            doc.setProperty("dublincore", "lastContributor", p.getName());
            doc.setProperty("dublincore", "contributors", new String[]{p.getName()});
        }
    }

}
