package com.directfn.cluster;

import com.directfn.oms.beans.User;
import org.jgroups.Address;

import java.io.Serializable;

public class UserSessionUpdate extends ClusterEvent implements Serializable {

    private boolean isLogin = false; //user this to identify wheter it was a login or logout.  true - login ,  false - logout
    private User user;
    private int authResult = -1;
    private String customerId = null;
    private String appSrvId = null;
    // private  String ip = null:
    String ipAdr = null;

    public String getIpAdr() {
        return ipAdr;
    }

    public void setIpAdr(String ipAdr) {
        this.ipAdr = ipAdr;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isLogin() {
        return isLogin;
    }

    public void setLogin(boolean login) {
        isLogin = login;
    }

    public int getAuthResult() {
        return authResult;
    }

    public void setAuthResult(int authResult) {
        this.authResult = authResult;
    }

    public String getCustomerId() { return customerId; }

    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getAppSrvId() { return appSrvId; }

    public void setAppSrvId(String appSrvId) { this.appSrvId = appSrvId; }

}
