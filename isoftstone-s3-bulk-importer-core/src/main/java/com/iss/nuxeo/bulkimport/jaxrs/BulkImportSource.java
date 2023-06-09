package com.iss.nuxeo.bulkimport.jaxrs;


import com.iss.nuxeo.bulkimport.ImportContext;
import com.iss.nuxeo.bulkimport.source.FileSourceManager;
import com.iss.nuxeo.bulkimport.source.FileSourceProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

import static com.iss.nuxeo.bulkimport.common.StringTool.toJson;

/**
 * Define bulk import source view
 * Listing the folder list under S3 bucket and update sublocation based on the selection of folder
 * bind view to the template file
 */
@WebObject(type="BulkImportSource")
@Produces("application/json; charset=UTF-8")
public class BulkImportSource extends DefaultObject {

    private static final Log log = LogFactory.getLog(BulkImportSource.class);
    @GET
    @Path("locations")
    public Response getSourceLocations() {
        try {
            FileSourceProvider provider = this.getFileSetProvider();

            String root = provider.getSourceRoot();
            String result = toJson(Collections.singletonMap("root", root));
            return Response.ok(result, MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            log.error("failed to retrieve import root location", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("sublocations")
    public Response getSourceSublocations(@QueryParam("baseDir") String baseDir) {

        if (log.isDebugEnabled()) {
            log.debug("baseDir: " + baseDir);
        }

        try {
            FileSourceProvider provider = getFileSetProvider();

            if (!provider.isValidSublocations(baseDir, true)) {
                return Response.status(403).entity(baseDir + " is not a valid folder for bulk import.").build();
            }

            List<String> list = provider.getSourceChildren(baseDir, true);

            return Response.ok(toJson(list)).build();
        } catch (Exception e) {
            log.error("failed to retrieve import sulocations", e);
            return Response.serverError().entity(e.getMessage()).build();
        }

    }

    protected FileSourceProvider getFileSetProvider() {

        ImportContext icxt = new ImportContext();
        icxt.setPrincipal(this.ctx.getPrincipal());

        FileSourceManager providers = Framework.getService(FileSourceManager.class);

        return  providers.getFileSourceProvider(icxt);

    }

}
