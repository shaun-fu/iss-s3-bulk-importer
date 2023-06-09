package com.iss.nuxeo.bulkimport.jaxrs;

import com.iss.nuxeo.bulkimport.ImportContext;
import com.iss.nuxeo.bulkimport.source.FileSourceManager;
import com.iss.nuxeo.bulkimport.source.FileSourceProvider;
import com.iss.nuxeo.bulkimport.task.TaskInfo;
import com.iss.nuxeo.bulkimport.task.TaskPool;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.importer.executor.jaxrs.HttpFileImporterExecutor;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;

/**
 *
 */
@WebObject(type = "BulkImportTask")
@Produces(MediaType.TEXT_PLAIN)
public class BulkImportTask extends DefaultObject {

    private static final Log log = LogFactory.getLog(BulkImportTask.class);

    @GET
    @Produces("application/json; charset=UTF-8")
    public List<TaskInfo> listTasks() {

        return TaskPool.get().listTasks();
    }

    /**
     * @param inputPath - S3 source bucket location
     * @param targetPath - shoot folder that initiate the import task
     * @return
     * Initiate import job - use default bulk import execute function
     */
    @GET
    @Path("run")
    @Produces("application/json; charset=UTF-8")
    public synchronized Response runImport(@QueryParam("inputPath") String inputPath,
                                           @QueryParam("targetPath") String targetPath, @QueryParam("batchSize") Integer batchSize,
                                           @QueryParam("nbThreads") Integer nbThreads, @QueryParam("interactive") Boolean interactive,
                                           @QueryParam("transactionTimeout") Integer transactionTimeout,
                                           @QueryParam("enableLogging") Boolean enableLogging) {

        ImportContext icxt = new ImportContext();
        icxt.setPrincipal(this.ctx.getPrincipal());

        FileSourceProvider provider = this.getFileSetProvider(icxt);

        boolean validInputPath = provider.isValidSublocations(inputPath, false);
        if (!validInputPath) {
            return Response.status(400).entity("Invalid input path [" + inputPath + "]").build();
        }

        HttpFileImporterExecutor executor = new HttpFileImporterExecutor();
        TaskInfo info = TaskPool.get().createTask(executor);
        if (info == null) {
            return Response.status(503)
                    .entity("The current running tasks has reached its maximum threshold " + TaskPool.get().getMaxTaskSize())
                    .build();
        }

        if (enableLogging) {
            executor.enableLogging();
        }
        String sourcePath = provider.createSourceNodePath(StringUtils.appendIfMissing(inputPath,"/"));
        String status = executor.run(null, null, sourcePath, targetPath, true, batchSize, nbThreads, interactive,
                transactionTimeout);

        info.setStartDate(new Date());
        info.setSource(inputPath);
        info.setTarget(targetPath);
        info.setUser(this.getContext().getPrincipal().getName());
        info.setStatus(status);

        if (log.isInfoEnabled()) {
            log.info("new bulk import task: " + info);
        }

        return Response.status(202).entity(info).build();

    }

    @Path("{taskId}/log")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getLog(@PathParam("taskId") String taskId) {

        HttpFileImporterExecutor task = TaskPool.get().getTaskExecutor(taskId);

        if (task == null) {
            return Response.status(404).entity("The task [" + taskId + "] has finished or doesn't exit").build();
        } else {
            return Response.ok().entity(task.getLogAsString()).build();
        }

    }

    @GET
    @Path("{taskId}/logActivate")
    public Response enableLogging(@PathParam("taskId") String taskId) {

        HttpFileImporterExecutor task = TaskPool.get().getTaskExecutor(taskId);

        if (task == null) {
            return Response.status(404).entity("The task [" + taskId + "] has finished or doesn't exit").build();
        } else {
            return Response.ok().entity(task.enableLogging()).build();
        }
    }

    @GET
    @Path("{taskId}/logDesactivate")
    public Response disableLogging(@PathParam("taskId") String taskId) {
        HttpFileImporterExecutor task = TaskPool.get().getTaskExecutor(taskId);

        if (task == null) {
            return Response.status(404).entity("The task [" + taskId + "] has finished or doesn't exit").build();
        } else {
            return Response.ok().entity(task.disableLogging()).build();
        }
    }

    @GET
    @Path("{taskId}/status")
    public Response getStatus(@PathParam("taskId") String taskId) {
        HttpFileImporterExecutor task = TaskPool.get().getTaskExecutor(taskId);

        if (task == null) {
            return Response.status(404).entity("The task [" + taskId + "] has finished or doesn't exit").build();
        } else {
            return Response.ok().entity(task.getStatus()).build();
        }
    }

    @GET
    @Path("{taskId}/running")
    public Response running(@PathParam("taskId") String taskId) {

        if (log.isDebugEnabled()) {
            log.debug("check task running status for " + taskId);
        }

        HttpFileImporterExecutor task = TaskPool.get().getTaskExecutor(taskId);

        if (task == null) {
            return Response.ok().entity("false").build();
        } else {
            return Response.ok().entity(task.running()).build();
        }
    }

    @GET
    @Path("{taskId}/kill")
    public Response kill(@PathParam("taskId") String taskId) {

        HttpFileImporterExecutor task = TaskPool.get().getTaskExecutor(taskId);

        if (task == null) {
            return Response.status(404).entity("The task [" + taskId + "] has finished or doesn't exit").build();
        } else {
            return Response.ok().entity(task.kill()).build();
        }
    }

    @GET
    @Path("{taskId}/waitForAsyncJobs")
    public Response waitForAsyncJobs(@PathParam("taskId") String taskId,
                                     @QueryParam("timeoutInSeconds") Integer timeoutInSeconds) {

        HttpFileImporterExecutor task = TaskPool.get().getTaskExecutor(taskId);

        if (task == null) {
            return Response.status(404).entity("The task [" + taskId + "] has finished or doesn't exit").build();
        } else {
            return task.waitForAsyncJobs(timeoutInSeconds);
        }

    }

    protected FileSourceProvider getFileSetProvider(ImportContext icxt) {

        FileSourceManager providers = Framework.getService(FileSourceManager.class);

        return  providers.getFileSourceProvider(icxt);

    }

}