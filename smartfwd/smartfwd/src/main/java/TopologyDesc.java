package apps.smartfwd.src.main.java;

import apps.smartfwd.src.main.java.models.SwitchPair;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.net.*;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static apps.smartfwd.src.main.java.constants.Env.N_SWITCH;


public class TopologyDesc {

    static final Logger logger= LoggerFactory.getLogger(TopologyDesc.class);
    public static final PortNumber INVALID_PORT=PortNumber.portNumber(0);
    static TopologyDesc instance;
    Set<Host> hosts=new HashSet<>();
    Map<Integer,DeviceId> switchIDToDeviceID =new HashMap<>();
    Map<DeviceId,Integer> deviceIDToSwitchID=new HashMap<>();
    Set<DeviceId> deviceIds=new HashSet<>();
    Map<SwitchPair,PortNumber> connectionPort=new HashMap<>();


    Map<DeviceId,Set<IpPrefix>> connectedIPs=new HashMap<>();
    Map<IpPrefix,DeviceId> ipConnectedDevices=new HashMap<>();



    DeviceService deviceService;
    HostService hostService;
    TopologyService topologyService;


    TopologyDesc(DeviceService deviceService, HostService hostService, TopologyService topoService){
        this.deviceService=deviceService;
        this.hostService=hostService;
        this.topologyService=topoService;
        for(Device device:deviceService.getAvailableDevices()){
            DeviceId deviceId=device.id();
            for(Host host:hostService.getConnectedHosts(deviceId)){
                for (Iterator<IpPrefix> it = host.ipAddresses().stream().map(IpAddress::toIpPrefix).iterator(); it.hasNext(); ) {
                    IpPrefix addr = it.next();
                    if(!connectedIPs.containsKey(deviceId)){
                        connectedIPs.put(deviceId,new HashSet<>());
                    }
                    connectedIPs.get(deviceId).add(addr);
                    ipConnectedDevices.put(addr,deviceId);
                }
            }
            hosts.addAll(hostService.getConnectedHosts(device.id()));

        }

        logger.info("Hosts:");
        hosts.forEach(host -> logger.debug(host.toString()));

        deviceService.getAvailableDevices().forEach(d-> deviceIds.add(d.id()));
        for (int i = 0; i < N_SWITCH; i++) {
            String rawStr = base16(i + 1);
            int zeroLen = 16 - rawStr.length();
            String res = "0".repeat(Math.max(0, zeroLen)) + rawStr;
            switchIDToDeviceID.put(i, DeviceId.deviceId("of:" + res));
            deviceIDToSwitchID.put(DeviceId.deviceId("of:" + res), i);
        }
        //connection port
        Topology topo=this.topologyService.currentTopology();
        TopologyGraph graph=this.topologyService.getGraph(topo);
        Set<TopologyEdge> edges = graph.getEdges();
        int num = 0;
        for (TopologyEdge edge : edges) {
            num++;
            ConnectPoint src = edge.link().src();
            ConnectPoint dst = edge.link().dst();
            connectionPort.put(SwitchPair.switchPair(src.deviceId(),dst.deviceId()),src.port());
            connectionPort.put(SwitchPair.switchPair(dst.deviceId(),src.deviceId()),dst.port());
        }
        logger.info("the numer of edge is " + num);
    }
    public Set<DeviceId> getDeviceIds(){
        return this.deviceIds;
    }
    public DeviceId getConnectedDeviceFromIp(IpPrefix addr){
        return ipConnectedDevices.get(addr);
    }

    public PortNumber getConnectionPort(DeviceId curr,DeviceId next){
        SwitchPair key=SwitchPair.switchPair(curr,next);
        if(connectionPort.containsKey(key)) {
            return connectionPort.get(key);
        }
        return INVALID_PORT;
    }
    public DeviceId getDeviceId(int switchNum){
        return switchIDToDeviceID.get(switchNum);
    }
    public DeviceId getDeviceId(String switchNum){
        return getDeviceId(Integer.parseInt(switchNum));
    }
    public int getDeviceIdx(DeviceId deviceId){
        return deviceIDToSwitchID.get(deviceId);
    }
//    public String getDeviceName(DeviceId deviceId){
//        return "s"+getDeviceIdx(deviceId);
//    }

    public Set<Host> getHosts() {
        return hosts;
    }

    public void updateConnectionPort() {
        connectionPort.clear();
        //connection port
        Topology topo=this.topologyService.currentTopology();
        TopologyGraph graph=this.topologyService.getGraph(topo);
        Set<TopologyEdge> edges = graph.getEdges();
        for (TopologyEdge edge : edges) {
            ConnectPoint src = edge.link().src();
            ConnectPoint dst = edge.link().dst();
            connectionPort.put(SwitchPair.switchPair(src.deviceId(),dst.deviceId()),src.port());
            connectionPort.put(SwitchPair.switchPair(dst.deviceId(),src.deviceId()),dst.port());
        }

    }

    public Set<IpPrefix> getConnectedIps(DeviceId did){
        if(!connectedIPs.containsKey(did)){
            connectedIPs.put(did,new HashSet<>());
        }
        return connectedIPs.get(did);
    }

    public static TopologyDesc getInstance(DeviceService deviceService, HostService hostService,TopologyService topologyService){
        if(null == instance) {
            instance=new TopologyDesc(deviceService,hostService,topologyService);
        }
        return instance;
    }
    public static TopologyDesc getInstance(){
        return instance;
    }

     String base16(int num) {
        if (num == 0) {
            return "0";
        }
        StringBuilder stringBuilder = new StringBuilder();
        while (num > 0) {
            int left = num % 16;
            if (left < 10) {
                stringBuilder.append(left);
            } else {
                char c = (char) ('a' + (left - 10));
                stringBuilder.append(c);
            }
            num = num / 16;
        }
        String res = stringBuilder.reverse().toString();
        return res;
    }
}
