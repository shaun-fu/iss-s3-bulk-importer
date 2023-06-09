package com.iss.nuxeo.bulkimport;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;


public class ImportContext {

    private NuxeoPrincipal principal;

    private String source;

    private String target;

    public NuxeoPrincipal getPrincipal() {
        return principal;
    }

    public void setPrincipal(NuxeoPrincipal principal) {
        this.principal = principal;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

}