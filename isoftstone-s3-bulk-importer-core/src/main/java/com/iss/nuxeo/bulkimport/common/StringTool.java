package com.iss.nuxeo.bulkimport.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class StringTool {

    private static final ObjectMapper om = new ObjectMapper();
    public static final String BLANK_STRING = "";

    private static final Log log = LogFactory.getLog(StringTool.class);
    public static long parseByteSize(String sizeStr, long defaultSize) {

        long size = defaultSize;

        if (isNotBlank(sizeStr)) {
            sizeStr = sizeStr.toUpperCase();
            long multiplex = 1l;
            if (sizeStr.endsWith("M")) {
                multiplex = 1024 * 1024L;
                sizeStr = sizeStr.substring(0, sizeStr.length() - 1);
            } else if (sizeStr.endsWith("G")) {
                multiplex = 1024 * 1024 * 1024L;
                sizeStr = sizeStr.substring(0, sizeStr.length() - 1);
            } else if (sizeStr.endsWith("T")) {
                multiplex = 1024 * 1024 * 1024 * 1024L;
                sizeStr = sizeStr.substring(0, sizeStr.length() - 1);
            } else if (sizeStr.endsWith("K")) {
                multiplex = 1024L;
                sizeStr = sizeStr.substring(0, sizeStr.length() - 1);
            }

            try {
                size = Long.parseLong(sizeStr) * multiplex;
            } catch (NumberFormatException e) {
                log.warn(
                        String.format("failed to parse [%s] as byte size, using default value [%s]instead.", sizeStr, defaultSize),
                        e);
            }
        }

        return size;
    }

    public static String toJson(Object obj) throws NuxeoException {

        try {
            return om.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn(String.format("failed to convert object %s to json, return null.", obj), e);
            return null;
        }
    }

    public static String stringifyObject(Object obj){
        if(obj==null){
            return BLANK_STRING;
        }else{
            return obj.toString();
        }
    }

}
