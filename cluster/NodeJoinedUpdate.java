package com.directfn.cluster;

import java.io.Serializable;

public class NodeJoinedUpdate extends ClusterEvent implements Serializable  {

    private String Ip = null;
    private String appSrvId = null;

    public String getIp() {
        return Ip;
    }

    public void setIp(String ip) {
        Ip = ip;
    }

    public String getAppSrvId() {
        return appSrvId;
    }

    public void setAppSrvId(String appSrvId) {
        this.appSrvId = appSrvId;
    }


}
