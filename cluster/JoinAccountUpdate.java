package com.directfn.cluster;

import java.io.Serializable;
import java.util.List;

public class JoinAccountUpdate extends ClusterEvent implements Serializable {

    private String customerId = null;
    private String appSrvId = null;
    private List<String> joinAccountCustomerIdList = null;

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getAppSrvId() {
        return appSrvId;
    }

    public void setAppSrvId(String appSrvId) {
        this.appSrvId = appSrvId;
    }

    public List<String> getJoinAccountCustomerIdList() {
        return joinAccountCustomerIdList;
    }

    public void setJoinAccountCustomerIdList(List<String> joinAccountCustomerIdList) {
        this.joinAccountCustomerIdList = joinAccountCustomerIdList;
    }
}
