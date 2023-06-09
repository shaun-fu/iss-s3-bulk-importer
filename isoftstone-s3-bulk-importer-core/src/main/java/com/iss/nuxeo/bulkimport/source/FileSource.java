package com.iss.nuxeo.bulkimport.source;

import com.iss.nuxeo.bulkimport.common.StringTool;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class FileSource {

    private boolean caseSensitive;

    private String rootLocation;

    private Set<String> excludes = new HashSet<String>();

    private Set<String> includes = new HashSet<String>();

    private Set<String> metadataFileExtensions = new HashSet<String>();

    public void setRootLocation(String rootLocation) {
        this.rootLocation = rootLocation;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(String caseSensitive) {
        this.caseSensitive = "true".equalsIgnoreCase(caseSensitive)?true:false;
    }

    public String getRootLocation() {
        return rootLocation;
    }

    public Set<String> getExcludes() {
        return excludes;
    }

    public void setExcludes(String excludes) {
        if(isBlank(excludes)) {
            return ;
        }
        this.excludes = Arrays.stream(excludes.split(","))
                .filter(StringUtils::isNotBlank)
                .map(c->c.trim())
                .collect(Collectors.toSet());
    }

    public Set<String> getIncludes() {
        return includes;
    }

    public void setIncludes(String includes) {
        if(isBlank(includes)) {
            return ;
        }
        this.includes = Arrays.stream(includes.split(","))
                .filter(StringUtils::isNotBlank)
                .map(c->c.trim())
                .collect(Collectors.toSet());
    }

    public Set<String> getMetadataFileExtensions() {
        return metadataFileExtensions;
    }

    public void setMetadataFileExtensions(String metadataFileExtensions) {

        if(isBlank(metadataFileExtensions)) {
            return ;
        }
        this.metadataFileExtensions = Arrays.stream(metadataFileExtensions.split(","))
                .filter(StringUtils::isNotBlank)
                .map(c->c.trim())
                .collect(Collectors.toSet());

    }

    @Override
    public String toString() {
        return StringTool.toJson(this);
    }


}
