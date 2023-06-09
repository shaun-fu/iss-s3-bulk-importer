package com.iss.nuxeo.bulkimport.source;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

import java.util.HashMap;
import java.util.Map;

@XObject("filesetProvider")
public class FileSourceProviderDescriptor implements Descriptor{

    @XNode("@id")
    private String id;

    @XNode("class")
    private Class<?> providerClass;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    private Map<String, String> properties = new HashMap<String, String>();

    @Override
    public String getId() {
        return id;
    }

    public Class<?> getProviderClass() {
        return providerClass;
    }

    public Map<String, String> getProperties() {
        return properties;
    }


}
