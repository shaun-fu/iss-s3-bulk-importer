package com.iss.nuxeo.bulkimport.jaxrs;

import com.iss.nuxeo.bulkimport.ImportContext;
import com.iss.nuxeo.bulkimport.source.FileSourceManager;
import com.iss.nuxeo.bulkimport.source.FileSourceProvider;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Launch issFileImport page
 * use @Path to bind URI to the method
 */
@WebObject(type= "bulkimport")
@Path("/issFileImporter")
@Produces("text/html; charset=UTF-8")
public class BulkImportWebRoot extends ModuleRoot {

    /**
     * @param targetPath destination where assets will get ingested to
     * @return
     * Use @QueryParam to inject targetPath to this method as a param
     */
    @GET
    public Object index(@QueryParam("targetPath") String targetPath) {
        FileSourceManager providers = Framework.getService(FileSourceManager.class);
        ImportContext icxt = new ImportContext();
        icxt.setPrincipal(this.ctx.getPrincipal());
        FileSourceProvider provider = providers.getFileSourceProvider(icxt);
        String root = provider.getSourceRoot();
        return getView("importForm").arg("targetPath", targetPath).arg("sourceLocations", root);

    }

    @Path("source")
    public Object source() {

        return newObject("BulkImportSource");

    }


    @Path("tasks")
    public Object tasks() {

        return newObject("BulkImportTask");

    }

}
