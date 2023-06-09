package com.iss.nuxeo.bulkimport.task;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.platform.importer.executor.jaxrs.HttpFileImporterExecutor;
import org.nuxeo.runtime.api.Framework;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TaskPool {

    //TODO: implement cluster wide max tasks.
    private Map<TaskInfo, HttpFileImporterExecutor> tasks = new HashMap<TaskInfo, HttpFileImporterExecutor>();

    private int maxTasks = 1; //default is 1.

    private static TaskPool instance;

    private TaskPool() {
        String max = Framework.getProperty("TASKS_MAX_SIZE_PROP");
        if(StringUtils.isNumeric(max)) {
            this.maxTasks = Integer.parseInt(max);
        }
    }

    public static synchronized TaskPool get() {
        if(instance == null) {
            instance = new TaskPool();
        }
        return instance;
    }

    public synchronized TaskInfo createTask(HttpFileImporterExecutor executor) {

        if(tasks.size()>=maxTasks) {
            clear();
        }

        if(tasks.size()>=maxTasks) {
            return null;
        }
        else {
            TaskInfo task = TaskInfo.asTaskInfo(this.generateTaskId());
            this.tasks.put(task, executor);
            return task;
        }
    }

    public HttpFileImporterExecutor getTaskExecutor(String taskId) {
        return this.tasks.get(TaskInfo.asTaskInfo(taskId));
    }

    public List<TaskInfo> listTasks() {

        List<TaskInfo> tasks =  new ArrayList<TaskInfo>();
        tasks.addAll(this.tasks.keySet());

        return tasks;
    }

    public int getMaxTaskSize() {
        return this.maxTasks;
    }

    private void clear() {
        tasks.values().removeIf(t->!t.isRunning());
    }

    public String generateTaskId() {
        return UUID.randomUUID().toString();
    }

}
