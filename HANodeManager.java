package com.directfn.oms.beans;

import com.directfn.cluster.NodeJoinedUpdate;
import com.directfn.cluster.OMSCluster;
import com.directfn.oms.util.SysPara;
import org.apache.log4j.Logger;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HANodeManager {

    final static Logger logger = Logger.getLogger(HANodeManager.class);
   // public static Map<String, String> mapIpToSrvId = new HashMap<String, String>();


    public static Map<String,String> mapCusIdToSrvId = new HashMap<String, String>(); //value of key is taken as XCHG_routing_ID | DSE_routing_Id

    public static void AddCusSrvEntrytoStore(String cusid,String appSrvId) {
        mapCusIdToSrvId.put(cusid,appSrvId );
    }

    public static void removeCusSrvEntryFromStore(String appSrvId) {
        logger.info("  disconnected node msg recv: remove customers for appsrvID : "+appSrvId);

        String AppSrvIdToBeRemoved = appSrvId;

        Iterator<Map.Entry<String, String> >
                iterator = mapCusIdToSrvId.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<String, String>
                    entry
                    = iterator.next();
            logger.info("check to be removed: "+entry.getValue());
            if (AppSrvIdToBeRemoved.equals(entry.getValue())) {
                logger.info("removed appsrvId : "+entry.getValue()+" cusId : "+entry.getKey());
                iterator.remove();
            }
            }

        logger.info(" removeCusSrvEntryFromStore DONE for appSrvId  : "+appSrvId);
    }

  /*  public static void AddIpAdrSrvIdEntrytoStore(String IP, String appSrvId) {
        logger.info("add mapping info to IP/ SrvID - appsrvid : "+appSrvId+" IP :: "+IP);
        mapIpToSrvId.put(IP,appSrvId);
    }

    public static void removeIpAdrSrvIdEntryFromStore(String Ip) {
        logger.info("remove mapping info to IP/ID appsrvid : "+Ip);
        mapIpToSrvId.remove(Ip);
    }*/


    public static String getAppSvrIdforCusID(String cusId) {
        logger.info("paased cus id from execution :"+cusId);
       String appSrvId = mapCusIdToSrvId.get(cusId);
        logger.info("paased cus id :"+cusId+"returned appsrv id :"+appSrvId);
        return appSrvId;
    }


    public static String getAppSvrIdfromIP(String IP) {
        //get key from value hashmap
        logger.info("getAppSvrIdfromIP mapping info; passed IP : "+IP);
        if(mapIpToSrvId.containsKey(IP)){
            for (Map.Entry<String,String> et : mapIpToSrvId.entrySet()){
               logger.info(" mapSrvIdToIpgetkey :"+et.getKey()+" mapSrvIdToIp getvalue :"+et.getValue());
            }
            logger.info(" passed IP & returned {appsrvID} : "+ mapIpToSrvId.get(IP));

            return mapIpToSrvId.get(IP);
       }
        return null;
    }

    public static void broadastSrvIdIpAdrEntry(String appSrvId, String ip) {
        if(SysPara.getParameter("OMS_CLUSTER_MODE", false).equalsIgnoreCase("1")) {
            NodeJoinedUpdate nodeJoinedUpdate = new NodeJoinedUpdate();
            nodeJoinedUpdate.setIp(ip);
            nodeJoinedUpdate.setAppSrvId(appSrvId);
            logger.info("--broadcastNodeJoinedUpdate-SrvId&IpAdrEntry--");
            OMSCluster.broadcastNodeJoinedUpdate(nodeJoinedUpdate);
        }
    }

}

