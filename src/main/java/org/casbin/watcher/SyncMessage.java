package org.casbin.watcher;

import java.util.List;

public class SyncMessage {
    public static final String OP_ADD_POLICY = "addP";
    public static final String OP_REMOVE_POLICY = "rmP";
    public static final String OP_ADD_FILTERED_POLICY = "addFP";
    public static final String OP_SAVE_POLICY = "saveP";

    private String instanceId;
    private String op;
    private List<String> contents;
    private Integer filterIndex;
    private String modelId;
    private String section;
    private String ptype;

    public SyncMessage() {

    }

    public SyncMessage(String instanceId, String modelId, String op, String section, String ptype, List<String> s) {
        this.instanceId = instanceId;
        this.modelId = modelId;
        this.op = op;
        this.section = section;
        this.ptype = ptype;
        this.contents = s;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public List<String> getContents() {
        return contents;
    }

    public void setContents(List<String> contents) {
        this.contents = contents;
    }

    public Integer getFilterIndex() {
        return filterIndex;
    }

    public void setFilterIndex(Integer filterIndex) {
        this.filterIndex = filterIndex;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getPtype() {
        return ptype;
    }

    public void setPtype(String ptype) {
        this.ptype = ptype;
    }

    @Override
    public String toString() {
        return "SyncMessage{" +
                "instanceId='" + instanceId + '\'' +
                ", op='" + op + '\'' +
                ", contents=" + contents +
                ", filterIndex=" + filterIndex +
                ", modelId='" + modelId + '\'' +
                ", section='" + section + '\'' +
                ", ptype='" + ptype + '\'' +
                '}';
    }
}
