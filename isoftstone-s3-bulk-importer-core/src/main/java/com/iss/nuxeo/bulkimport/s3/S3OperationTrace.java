package com.iss.nuxeo.bulkimport.s3;

import com.amazonaws.services.s3.model.ObjectMetadata;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;

import static com.iss.nuxeo.bulkimport.common.StringTool.toJson;

public class S3OperationTrace {

    public static enum TraceLevel {
        NONE, INFO, VERBOSE;

        public static TraceLevel fromString(String level) {
            switch (level == null ? "none" : level.toLowerCase()) {
                case "none":
                    return NONE;
                case "info":
                    return INFO;
                case "verbose":
                    return VERBOSE;
                default:
                    return NONE;
            }
        }
    }

    public static final String renameToDigest = "rename to digest";

    public static final String computeMD5 = "compute md5";

    public static final String rename = "rename";

    public static final String copy = "copy";

    public static final String move = "move";

    private static final String OPERATION_START_MESSAGE = "%1$s : start > source [/%2$s/%3$s]";

    private static final String OPERATION_END_MESSAGE = "%1$s : end > finished in %2$s ms, source [/%3$s/%4$s], target[/%5$s/%6$s], final digest [%7$s]";

    private static final String VERBOSE_OPERATION_END_MESSAGE = "%1$s : end > finished in %2$s ms, source [/%3$s/%4$s], target[/%5$s/%6$s], final digest [%7$s], final metadata -> %8$s";

    private static final String STEP_START_MESSAGE = "%1$s - %2$s : start > source [/%3$s/%4$s], target[/%6$s/%7$s], source digest [%8$s]";

    private static final String VERBOSE_STEP_START_MESSAGE = "%1$s - %2$s : start > source [/%3$s/%4$s], target[/%6$s/%7$s], source digest [%8$s], source metadata -> %5$s";

    private static final String STEP_END_MESSAGE = "%1$s - %2$s : end > finished in %3$s ms, source [/%4$s/%5$s], target[/%6$s/%7$s], target digest [%8$s]";

    private static final String VERBOSE_STEP_END_MESSAGE = "%1$s - %2$s : end > finished in %3$s ms, source [/%4$s/%5$s], target[/%6$s/%7$s], target digest [%8$s], tartet metadata ->, %9$s";

    private Log log;

    private String operation;

    private TraceLevel traceLevel = TraceLevel.NONE;

    private StopWatch operationStopWatch;

    private StopWatch stepStopWatch;

    private S3OperationTrace(Log log) {
        this.log = log;
    }

    public static S3OperationTrace get(Log log, String operation, TraceLevel traceLevel) {

        S3OperationTrace trace = new S3OperationTrace(log);
        trace.operation = operation;
        trace.traceLevel = traceLevel;
        trace.operationStopWatch = new StopWatch();
        trace.stepStopWatch = new StopWatch();
        return trace;
    }

    public void operationBegins(String bucket, String key) {
        if (traceLevel != TraceLevel.NONE) {
            this.operationStopWatch.start();
            String s = String.format(OPERATION_START_MESSAGE, operation, bucket, key);
            log(s);
        }
    }

    public void operationEnds(String sourceBucket, String sourceKey, String targetBucket, String targetKey,
                              ObjectMetadata finalMeta) {
        if (traceLevel != TraceLevel.NONE) {
            this.operationStopWatch.stop();
            String template = (traceLevel == TraceLevel.VERBOSE) ? VERBOSE_OPERATION_END_MESSAGE : OPERATION_END_MESSAGE;
            String meta = (traceLevel == TraceLevel.VERBOSE) ? toJson(finalMeta) : null;
            String s = String.format(template, operation, operationStopWatch.getTime(), sourceBucket, sourceKey, targetBucket,
                    targetKey, finalMeta.getETag(), meta);
            log(s);
        }
    }

    public void stepBegins(String step, String sourceBucket, String sourceKey, ObjectMetadata sourceMeta,
                           String targetBucket, String targetKey) {

        if (traceLevel != TraceLevel.NONE) {

            this.stepStopWatch.reset();
            this.stepStopWatch.start();

            String template = (traceLevel == TraceLevel.VERBOSE) ? VERBOSE_STEP_START_MESSAGE : STEP_START_MESSAGE;
            String meta = (traceLevel == TraceLevel.VERBOSE) ? toJson(sourceMeta) : null;

            String s = String.format(template, operation, step, sourceBucket, sourceKey, meta, targetBucket, targetKey,
                    sourceMeta.getETag());
            log(s);
        }
    }

    public void stepEnds(String step, String sourceBucket, String sourceKey, String targetBucket, String targetKey,
                         ObjectMetadata targetMeta) {

        if (traceLevel != TraceLevel.NONE) {

            this.stepStopWatch.stop();

            String template = (traceLevel == TraceLevel.VERBOSE) ? VERBOSE_STEP_END_MESSAGE : STEP_END_MESSAGE;
            String meta = (traceLevel == TraceLevel.VERBOSE) ? toJson(targetMeta) : null;

            String s = String.format(template, operation, step, stepStopWatch.getTime(), sourceBucket, sourceKey,
                    targetBucket, targetKey, targetMeta.getETag(), meta);
            log(s);
        }
    }

    public void log(String s) {

        if (log.isTraceEnabled()) {
            log.trace(s);
        } else if (log.isDebugEnabled()) {
            log.debug(s);
        } else if (log.isInfoEnabled()) {
            log.info(s);
        } else if (log.isWarnEnabled()) {
            log.warn(s);
        } else if (log.isErrorEnabled()) {
            log.error(s);
        } else if (log.isFatalEnabled()) {
            log.fatal(s);
        }
    }
}
