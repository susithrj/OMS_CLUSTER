package com.directfn.cluster;

import com.directfn.messageProtocol.api.*;
import com.directfn.messageProtocol.impl.beans.EnvelopeImpl;
import com.directfn.messageProtocol.impl.beans.HeaderImpl;
import com.directfn.messageProtocol.impl.beans.ver1.authentication.LogoutResponseBean;
import com.directfn.oms.beans.HANodeManager;
import com.directfn.oms.mqao.TRSQStore;
import com.directfn.oms.ngp.writer.api.TrsWriterFactory;
import com.directfn.oms.ngp.writer.api.exceptions.TrsWriterException;
import com.directfn.oms.util.CustomerStore;
import com.directfn.oms.util.TRSManager;
import com.directfn.oms.util.UserManager;
import com.directfn.trs.beans.TRSMessage;
import com.directfn.trs.beans.TRSMeta;
import org.apache.log4j.Logger;
import org.jgroups.*;

import java.net.InetAddress;
import java.util.*;

public class OMSCluster extends ReceiverAdapter {

    final static transient Logger logger = Logger.getLogger(OMSCluster.class);

    private static JChannel channel;
    private static List<Address> previousNodeList = new ArrayList<Address>();
    private String primaryNode;
    private boolean isPrimary = false;
    private boolean isSingleNodeYet = false;
    private static OMSCluster omsCluster;


    public OMSCluster() {
        try {
            channel = new JChannel("tcp.xml"); //configure with xml, default configuration set to multicast to 10.1.200.0
            channel.setName(InetAddress.getLocalHost().getHostName() + "_" +System.getProperty("APPSVR_ID"));
            channel.connect(System.getProperty("CLUSTER_ID"));
            channel.setReceiver(this);
            channel.setDiscardOwnMessages(true);
            logger.info("New JChannel created with member:" + channel.getName()+ " ");

            previousNodeList = channel.getView().getMembers();
            logger.info("Current cluster size: " + previousNodeList.size());
            updateClusterMembers(previousNodeList);

            if ((previousNodeList.size() == 1)) {
                isPrimary = true;
                isSingleNodeYet = true;
                channel.setDiscardOwnMessages(false);
            } else if (previousNodeList.size() > 1) {
                askClusterThatWhoIsPrimary();
            }
        } catch (Exception e) {
            logger.error("Error occurred while initializing OMS cluster. OMS host:"+ channel.getName(),e);
        }
        logger.info("AM I THE PRIMARY: "+isPrimary);
        String appSrvId = System.getProperty("APPSVR_ID");
            String ip = channel.getAddressAsString();
            if(ip != null){
                logger.info("init OMSCluster node IP : "+ip);
            }
            if(appSrvId != null){
                logger.info("init OmsCluster node appsrvid : "+appSrvId);
            }
            if(ip != null && appSrvId !=null){
                HANodeManager.AddIpAdrSrvIdEntrytoStore(String.valueOf(ip),appSrvId);
                logger.info(" broadcasting initiated OMS Cluster node ");
                HANodeManager.broadastSrvIdIpAdrEntry(appSrvId, String.valueOf(ip));
            }
    }

    public synchronized static OMSCluster getInstance() {
        if(omsCluster == null){
            omsCluster = new OMSCluster();
        }
        return omsCluster;
    }

//    public void closeChannel() {
//        this.channel.close();
//    }

    public void viewAccepted(View view) {

        boolean isJointEvent = true;
        List<Address> currentNodeList = view.getMembers();
        if (currentNodeList.size() > previousNodeList.size()) {
            isJointEvent = true;
            if (logger.isDebugEnabled()) {
                logger.debug("\n======== Updating cluster members after join:");
            }
            updateClusterMembers(currentNodeList);
            if (!channel.getDiscardOwnMessages()) {
                channel.setDiscardOwnMessages(true);
                isSingleNodeYet = false;
            }

            logger.info("A new member has been added to the cluster");
            if (isPrimary) {
                // send broadcast message to all cluster members to update about their master
                sendMessage(new IamPrimary());
            }
            for (Address adr: previousNodeList) {
                if (!(currentNodeList.contains(adr))) {
                    String appSrvId = HANodeManager.getAppSvrIdfromIP(String.valueOf(adr));
                    logger.info(" viewAccepted() - new node joined - adding to IP-Srvid Map : "+adr.toString()+" appsrvID : "+appSrvId);
                     HANodeManager.AddIpAdrSrvIdEntrytoStore(String.valueOf(adr),appSrvId);
                }
            }


        } else if (currentNodeList.size() < previousNodeList.size()) {
            isJointEvent = false;
            if (logger.isDebugEnabled()) {
                logger.debug("\n======== Updating cluster members after disconnect:");
            }
            updateClusterMembers(currentNodeList);
            logger.info("A member has been disconnected from the cluster");
            if (!isPrimaryHostStillAlive(currentNodeList, primaryNode)) {
                logger.info("====== As Primary host is down I am becoming the master =======");
                isPrimary = true;
                sendMessage(new IamPrimary());
            }

            for (Address adr: previousNodeList) {
                if (!(currentNodeList.contains(adr))) {
                    logger.info("Ip of node left"+adr.toString());
               String appSrvId =  HANodeManager.getAppSvrIdfromIP(String.valueOf(adr));
                    HANodeManager.removeCusSrvEntryFromStore(appSrvId);
                    HANodeManager.removeIpAdrSrvIdEntryFromStore(String.valueOf(adr));
                }
            }

            notifyClusterStatusTRS();

            notifyClusterStatusWSTRS();

        } else {
            //Assumes that both app is up since previous nodes=current nodes
        }
        previousNodeList = currentNodeList;
        logger.info("AM I THE PRIMARY:"+isPrimary);
    }

    private void notifyClusterStatusWSTRS() {

        TrsWriterFactory trsWriterFactory = TrsWriterFactory.getInstance();
        MessageProtocolFacade messageProtocolFacade = MessageProtocolFacadeFactory.getInstance().getMessageProtocolFacade();

        LogoutResponseBean logoutResponseBean = new LogoutResponseBean();
        logoutResponseBean.setLogoutStatus(String.valueOf(true));

        com.directfn.messageProtocol.api.beans.Header header = new HeaderImpl();
        header.setMsgGroup(GroupConstants.GROUP_AUTHENTICATION);
        header.setMsgType(MessageConstants.RESPONSE_TYPE_LOGOUT_ALL);
        header.setRespStatus(ValueConstants.RESPONSE_STATUS_SUCCESS);

        try {
            List<Integer> wsTRSInstanceIDs = TRSManager.getAllWsTrsInstanceIdNumbers();
            trsWriterFactory.getTrsWriter().
                    writeAuthResponse(messageProtocolFacade.getJSonString( new EnvelopeImpl((HeaderImpl) header, logoutResponseBean)),wsTRSInstanceIDs);
        } catch (TrsWriterException e) {
            logger.error("Error processing client request: " + header.getMsgGroup() + "-" + header.getMsgType(), e);
            header.setRespStatus(ValueConstants.RESPONSE_STATUS_FAILURE);
            header.setErrorCode(ValueConstants.ERR_SYSTEM_ERROR);
        }
    }

    private void notifyClusterStatusTRS() {

        TRSMessage trsMessage = new TRSMessage();
        trsMessage.sMessageType = ""+ TRSMeta.MT_CLUSTER_OMS_STATUS;
        trsMessage.sMessageData = "-1"; //-1 means a node has left.
        trsMessage.sTRSID = "TRS01";
        TRSQStore.getInstance().send(trsMessage.sTRSID, trsMessage.toString(), TRSMeta.MT_CLUSTER_OMS_STATUS);
    }

    private void updateClusterMembers(List<Address> currentNodeList) {
        List<String> memberNames = new ArrayList();
        if (logger.isDebugEnabled()) {
            logger.debug("\n====== updating cluster members:");
        }
        for (Address address : currentNodeList) {
            if (logger.isDebugEnabled()) {
                logger.debug(address.toString() + ",");
            }
            memberNames.add(address.toString());
        }
    }

    public void receive(Message msg) {

        try {
            String messageSource = msg.getSrc().toString();
            Object messageObject = msg.getObject(Thread.currentThread().getContextClassLoader());

            if (messageObject instanceof IamPrimary) {

                if (logger.isDebugEnabled()) {
                    logger.debug("Setting primary node to " + messageSource);
                }
                if (!isSingleNodeYet) {
                    isPrimary = false;
                }
                primaryNode = messageSource;
            } else if (messageObject instanceof WhoIsPrimary) {
                Class.forName("com.directfn.cluster.WhoIsPrimary");

                logger.info("Receiving who is primary message.");
                if (isPrimary == true) {
                    sendMessage(new IamPrimary());
                    logger.info("Replying that I am primary.");
                }
            }
            else if(messageObject instanceof UserSessionUpdate){ //received user session update message
                UserSessionUpdate userSessionUpdate = (UserSessionUpdate)messageObject;

                logger.info("session update received. can manage multiple sessions now for: "+userSessionUpdate.getUser());
                if(userSessionUpdate.isLogin()){
                    logger.info("UserSessionUpdate received for LOGIN");
                    //login happened in remote oms
                    UserManager.addUser(userSessionUpdate.getUser(),userSessionUpdate.getAuthResult(),null);

                    if(userSessionUpdate.getAppSrvId() != null && userSessionUpdate.getCustomerId() != null){
                        logger.info(" session update recived for LOGIN with cusid and appsrvid - appsrvid: "+userSessionUpdate.getAppSrvId()+" cusID : "+userSessionUpdate.getCustomerId());
                        HANodeManager.AddCusSrvEntrytoStore( userSessionUpdate.getCustomerId(),userSessionUpdate.getAppSrvId());
                        //need to brodcast ip of (early started) node to newly connected nodes
                       if(!HANodeManager.mapIpToSrvId.containsKey(userSessionUpdate.getIpAdr())){
                           logger.info(" session update recived for LOGIN : update IpAdr SrvId Entry to Store: "+userSessionUpdate.getIpAdr()+" AppsrvID : "+userSessionUpdate.getAppSrvId());
                           HANodeManager.AddIpAdrSrvIdEntrytoStore(userSessionUpdate.getIpAdr(),userSessionUpdate.getAppSrvId());
                       }
                    }
                }
                else{
                    //logout happened in remote oms
                    UserManager.removeUser(userSessionUpdate.getUser().sessionID);
                }
            }
            else if (messageObject instanceof NodeJoinedUpdate){
            NodeJoinedUpdate nodeJoinedUpdate = (NodeJoinedUpdate) messageObject;
           if( nodeJoinedUpdate.getIp() != null && nodeJoinedUpdate.getAppSrvId() != null ) {
               logger.info("receive node joined update with appsrvid and ip :"+ nodeJoinedUpdate.getAppSrvId()+" IP :"+ nodeJoinedUpdate.getIp() );
               HANodeManager.AddIpAdrSrvIdEntrytoStore(nodeJoinedUpdate.getIp(),nodeJoinedUpdate.getAppSrvId());
            }
            }
            else if (messageObject instanceof JoinAccountUpdate){
                JoinAccountUpdate joinAccountUpdate = (JoinAccountUpdate) messageObject;
                if(joinAccountUpdate.getAppSrvId() != null && joinAccountUpdate.getCustomerId() != null){
                    logger.info("Adding Join Account Customer Id: "+joinAccountUpdate.getCustomerId());
                    CustomerStore.addJoinAccountCustomersIds(joinAccountUpdate.getCustomerId(), joinAccountUpdate.getJoinAccountCustomerIdList());
                }
            }
        } catch (Exception e) {
            logger.info("\n=== OMSCluster Message Receiver,Unknown Msg Received:" + e.getMessage());
        }
        logger.info("AM I THE PRIMARY:"+isPrimary);
    }


    public void askClusterThatWhoIsPrimary() {
        sendMessage(new WhoIsPrimary());
    }

    private boolean isPrimaryHostStillAlive(List<Address> list, String nodeName) {
        Iterator<Address> it = list.iterator();
        while (it.hasNext()) {
            if (it.next().toString().equals(nodeName)) {
                return true;
            }
        }
        return false;
    }


    public boolean getIsPrimary() {
        return isPrimary;
    }

    public String getPrimaryNode() {
        return this.primaryNode;
    }

    private void sendMessage(ClusterEvent event) {
        try {
            Message msg = new Message(null, event);
            channel.send(msg);
        } catch (Exception e) {
            logger.error(e);
        }
    }

    public void broadcastUserSessionDetails(ClusterEvent event) {
        try {
            logger.info("broadCast UserSessionDetails");
            Message msg = new Message(null, event);
            channel.send(msg);
        } catch (Exception e) {
            logger.error(e);
        }
    }

    public static void broadcastNodeJoinedUpdate(ClusterEvent event) {
        try {
            logger.info("broadcastNodeJoinedUpdate");
            Message msg = new Message(null, event);
            channel.send(msg);
        } catch (Exception e) {
            logger.error(e);
        }
    }

    public static void broadcastJoinAccountUpdate(ClusterEvent event) {
        try {
            Message msg = new Message(null, event);
            channel.send(msg);
        } catch (Exception e) {
            logger.error(e);
        }
    }

    public String getIpAdr() {
        logger.info("adr :"+channel.getAddressAsString());
        return channel.getAddressAsString();
    }
}
