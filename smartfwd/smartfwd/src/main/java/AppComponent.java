/*
 * Copyright 2020 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package apps.smartfwd.src.main.java;

import apps.smartfwd.src.main.java.constants.App;
import apps.smartfwd.src.main.java.constants.FlowEntryPriority;
import apps.smartfwd.src.main.java.constants.FlowEntryTimeout;
import apps.smartfwd.src.main.java.constants.Env;
import apps.smartfwd.src.main.java.models.SwitchPair;
import apps.smartfwd.src.main.java.task.*;
import apps.smartfwd.src.main.java.task.base.PeriodicalTask;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.*;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.CoreService;
import org.onosproject.net.*;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.edge.EdgePortService;
import org.onosproject.net.flow.*;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.statistic.PortStatisticsService;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static apps.smartfwd.src.main.java.TopologyDesc.INVALID_PORT;


/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
public class AppComponent {

    private final  Logger logger = LoggerFactory.getLogger(getClass());

    protected static KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(Integer.class)
            .register(DeviceId.class)
            .build("smartfwd");

    @Reference(cardinality = org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY)
    protected CoreService coreService;
    @Reference(cardinality = org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY)
    protected TopologyService topologyService;
    @Reference(cardinality = org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY)
    protected PacketService packetService;
    @Reference(cardinality = org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY)
    protected HostService hostService;
    @Reference(cardinality = org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY)
    protected FlowObjectiveService flowObjectiveService;
    @Reference(cardinality = org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;
    @Reference(cardinality = org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;
    @Reference(cardinality = org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY)
    protected EdgePortService edgePortService;
    @Reference(cardinality = org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY)
    protected PortStatisticsService portStatisticsService;
    @Reference(cardinality = org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY)
    protected GroupService groupService;

    BlockingQueue<FlowTableEntry> flowEntries=new LinkedBlockingQueue<>();

    FlowEntryTask flowEntryTask;
    SocketServerTask classifierServer;
    private ArrayList<FlowRule> defaultFlowRulesCache = new ArrayList<>();
    private ArrayList<FlowRule> optiFlowRulesCache = new ArrayList<>();
    private HashMap<Integer, List<AflowEntry>> flowMap = new HashMap<>();
    String topoIdxJson = "{ \"topo_idx\" : 0}";
    String rateFilePath = "";


    PeriodicalSocketClientTask.RequestGenerator defaultIdxRouteReq = new PeriodicalSocketClientTask.RequestGenerator() {
        @Override
        public String payload() {
            return topoIdxJson + "*";
        }
    };
    PeriodicalSocketClientTask.ResponseHandler defaultIdxRouteHandler = new PeriodicalSocketClientTask.ResponseHandler() {
        @Override
        public void handle(String payload) {
            //clear default route
            logger.info("request default routing");
            emptyDefaultFlow();
            TopologyDesc topo=TopologyDesc.getInstance();
            try{
                ObjectMapper mapper=new ObjectMapper();
                JsonNode root=mapper.readTree(payload);
                JsonNode routings=root.get("res1");
                logger.info("routings size:" + routings.size());
                for(int i=0;i<routings.size();i++){
                    JsonNode routing=routings.get(i);
                    int start=routing.get(0).asInt();
                    int end=routing.get(routing.size()-1).asInt();
                    for(IpPrefix srcAddr:topo.getConnectedIps(topo.getDeviceId(start))){
                        for(IpPrefix dstAddr:topo.getConnectedIps(topo.getDeviceId(end))){
                            for(int j=0;j<routing.size()-1;j++){
                                int curr=routing.get(j).asInt();
                                int next=routing.get(j+1).asInt();
                                DeviceId currDeviceId=topo.getDeviceId(curr);
                                DeviceId nextHopDeviceId=topo.getDeviceId(next);
                                PortNumber output=topo.getConnectionPort(currDeviceId,nextHopDeviceId);
                                if(output.equals(INVALID_PORT)){
                                    continue;
                                    //todo log
                                }
                                FlowTableEntry entry=new FlowTableEntry();
                                entry.setDeviceId(currDeviceId)
                                        .setPriority(FlowEntryPriority.TABLE2_DEFAULT_ROUTING)
                                        .setTable(2);
                                entry.filter()
                                        .setSrcIP(srcAddr)
                                        .setDstIP(dstAddr);
                                entry.action()
                                        .setOutput(output);
                                FlowRule rule = entry.install(flowRuleService);
                                defaultFlowRulesCache.add(rule);
                            }
                        }
                    }


                }
                logger.info("---------default routing have been installed----------");
            }
            catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    };

    SocketServerTask.Handler classifierHandler= payload -> {
        //start new socket client
//        logger.info("classifier payload {}",payload);
        TopologyDesc topo=TopologyDesc.getInstance();
        JsonNode specifierNode;
        JsonNode statsNode;
        try {
            ObjectMapper mapper=new ObjectMapper();
            JsonNode root=mapper.readTree(payload);
            specifierNode=root.get("specifier");
            statsNode=root.get("stats");
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return;
        }
        ObjectMapper mapper=new ObjectMapper();
        String newPayload;
        try {
            ObjectNode node=mapper.createObjectNode();
            node.put("stats",statsNode);
            newPayload=mapper.writeValueAsString(node)+"*";
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return;
        }
        SocketClientTask task=new SocketClientTask(newPayload, response -> {
            //parse response and install flow entry
//            logger.info("classifier response {}",response);
            ObjectMapper m=new ObjectMapper();
            try {
                JsonNode resNode=m.readTree(response);
                int vlanId=resNode.get("res").asInt();
                int sport=specifierNode.get(0).asInt();
                int dport=specifierNode.get(1).asInt();
                String srcIp=specifierNode.get(2).asText();
                IpPrefix sip=IpAddress.valueOf(srcIp).toIpPrefix();
                String dstIp=specifierNode.get(3).asText();
                IpPrefix dip=IpAddress.valueOf(dstIp).toIpPrefix();

                String proto=specifierNode.get(4).asText();//TCP or UDP
                FlowTableEntry entry=new FlowTableEntry();
                DeviceId deviceId=topo.getConnectedDeviceFromIp(sip);
                if(null==deviceId) return;
                entry.setDeviceId(deviceId)
                        .setPriority(FlowEntryPriority.TABLE0_TAG_FLOW)
                        .setTable(0)
                        .setTimeout(FlowEntryTimeout.TAG_FLOW);
                entry.filter()
                        .setVlanId(5)
                        .setSrcIP(sip)
                        .setDstIP(dip)
                        .setDstPort(dport)
                        .setSport(sport)
                        .setProtocol((proto.equalsIgnoreCase("TCP")?IPv4.PROTOCOL_TCP: IPv4.PROTOCOL_UDP));
                entry.action()
                        .setVlanId(vlanId)
                        .setTransition(1);

                flowEntries.put(entry);
            } catch (JsonProcessingException | InterruptedException e) {
                e.printStackTrace();
            }
        },App.ALG_CLASSIFIER_IP,App.ALG_CLASSIFIER_PORT);
        task.start();
    };
    AtomicReference<List<Map<SwitchPair,Long>>>  collectedStats=new AtomicReference<>();
    AtomicReference<Map<SwitchPair,Long>> portRate=new AtomicReference<>();

    TrafficMatrixCollector.Handler trafficMatrixCollectorHandler=new TrafficMatrixCollector.Handler() {
        @Override
        public void handle(List<Map<SwitchPair, Long>> stats) {
            collectedStats.set(stats);
        }
    };
    TrafficMatrixCollector trafficMatrixCollector;



    PeriodicalSocketClientTask.ResponseHandler optRoutingRespHandler = resp -> {
        writeTimeHeaderToFile("/data/theMaxRate.txt");
        int topo_idx = 0;
        try {
            ObjectMapper mapper=new ObjectMapper();
            JsonNode topoIdx = mapper.readTree(topoIdxJson);
            topo_idx = topoIdx.get("topo_idx").asInt();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        writeToFile("当前TOPO:" + topo_idx, "/data/theMaxRate.txt");
        writeToFile("优化前:" + getCurrentMaxRate().toString() + " Mbit/s", "/data/theMaxRate.txt");
        writeToFile("优化前:" + getAllRate(), "/data/matchRate.txt");
        emptyOptiFlow();
        TopologyDesc topo=TopologyDesc.getInstance();
        ObjectMapper mapper=new ObjectMapper();
        try {
            JsonNode root=mapper.readTree(resp);
            for(int i=0;i<1;i++){
                JsonNode node=root.get("res1");
                for(int k=0;k<node.size();k++){
                    JsonNode routing=node.get(k);
                    int start=routing.get(0).asInt();
                    int end=routing.get(routing.size()-1).asInt();
                    //添加代码
                    if((start == 0 && end == 18) || (start == 4 && end == 18) || (start == 11 && end == 18)) {
                        int size = routing.size() + 2;
                        int[] to = new int[size];
                        int[] back = new int[size];
                        int cnt = 0;
                        int cntb = size - 1;
                        for(int j=0;j<routing.size();j++){
                            int curr=routing.get(j).asInt();
                            if (j == 0 || j == routing.size() - 1) {
                                to[cnt++] = curr;
                                back[cntb--] = curr;
                            }
                            to[cnt++] = curr;
                            back[cntb--] = curr;
                        }
                        if(start == 0) {
                            installOnePath(to, "192.168.2.101/32");
                            installOneBackPath(back, "192.168.2.101/32");
                        }
                        if(start == 4) {
                            installOnePath(to, "192.168.2.103/32");
                            installOneBackPath(back, "192.168.2.103/32");
                        }
                        if(start == 11) {
                            installOnePath(to, "192.168.2.104/32");
                            installOneBackPath(back, "192.168.2.104/32");
                        }
                        logger.info("%%%%%%%%%%%%%%%%%%" + Arrays.toString(to));
                    }


                    for(IpPrefix srcAddr:topo.getConnectedIps(topo.getDeviceId(start))){
                        for(IpPrefix dstAddr:topo.getConnectedIps(topo.getDeviceId(end))){
                            for(int j=0;j<routing.size()-1;j++){
                                int curr=routing.get(j).asInt();
                                int next=routing.get(j+1).asInt();
                                DeviceId currDeviceId=topo.getDeviceId(curr);
                                DeviceId nextHopDeviceId=topo.getDeviceId(next);
                                PortNumber output=topo.getConnectionPort(currDeviceId,nextHopDeviceId);
                                if(output.equals(INVALID_PORT)){
                                    continue;
                                    //todo log
                                }
                                FlowTableEntry entry=new FlowTableEntry();
                                entry.setDeviceId(currDeviceId)
                                        .setPriority(FlowEntryPriority.TABLE2_OPT_ROUTING)
                                        .setTable(2);
                                entry.filter()
                                        .setSrcIP(srcAddr)
                                        .setDstIP(dstAddr)
                                        .setVlanId(i);
                                entry.action()
                                        .setOutput(output);
                                FlowRule rule = entry.install(flowRuleService);
                                optiFlowRulesCache.add(rule);
                            }
                        }
                    }

                }
            }
            logger.info("---------opt routing have been installed----------");
            try {
                Thread.sleep(12000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            writeTimeHeaderToFile("/data/theMaxRate.txt");
            writeToFile("优化后：" + getCurrentMaxRate().toString() + " Mbit/s", "/data/theMaxRate.txt");
            writeToFile("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%", "/data/theMaxRate.txt");
            writeToFile("优化后：" + getAllRate(), "/data/matchRate.txt");
//            writeToFile("after", rateFilePath);
//            rateToFile(portRate, rateFilePath);
            try {
                Thread.sleep(120000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            emptyOptiFlow();

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    };
    PeriodicalSocketClientTask.RequestGenerator optRoutingReqGenerator=new PeriodicalSocketClientTask.RequestGenerator() {
        @Override
        public String payload() {
            List<Map<SwitchPair,Long>> traffic=collectedStats.get();
            if(null==traffic){
                return null;
            }
            TopologyDesc topo=TopologyDesc.getInstance();
            Map<String,ArrayList<Long>> content=new HashMap<>();
            for(int f=0;f<Env.N_FLOWS;f++){
                content.put(String.valueOf(f),new ArrayList<>());
                ArrayList<Long> stats=content.get(String.valueOf(f));
                for(int i=0;i<Env.N_SWITCH;i++){
                    for(int j=0;j<Env.N_SWITCH;j++){
                        if(i==j) continue;
                        SwitchPair switchPair = SwitchPair.switchPair(topo.getDeviceId(i),topo.getDeviceId(j));
                        Long res=traffic.get(f).get(switchPair);
                        if(res == null) {
                            logger.info("i:" + String.valueOf(i) + "j:" + j);
                            res = 0L;
                        }
                        stats.add(res);
                    }
                }
            }

//            logger.info(content.toString());
            JsonObject matrixRes = new JsonObject();
            JsonArray jsonArray = new JsonArray();
            for(int f = 0; f < Env.N_FLOWS; f++) {
                for(Long value : content.get(String.valueOf(f))) {
                    jsonArray.add(value);
                }
            }
            matrixRes.set("volumes", jsonArray);
            ObjectMapper mapper=new ObjectMapper();
            try {
                JsonNode topoIdx = mapper.readTree(topoIdxJson);
                int topo_idx = topoIdx.get("topo_idx").asInt();
                matrixRes.set("topo_idx", topo_idx);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            String payload=matrixRes.toString();
            return payload+"*";
        }
    };

    PortStatsCollectTask portRateCollector;
    PortStatsCollectTask.Consumer portRateConsumer=new PortStatsCollectTask.Consumer() {
        @Override
        public void consume(Map<SwitchPair, Long> stats) {
            portRate.set(stats);
            long time = new Date().getTime();
            rateFilePath = "/data/" + String.valueOf(time) + ".rate";
            rateToFile(portRate, rateFilePath);
            writeToFile(String.valueOf(getCurrentMaxRate()), "/data/currentMaxRate.txt");
        }
    };


    SocketClientTask.ResponseHandler connectionDownHandler = response -> {
        //清空优化路由流表项
        emptyOptiFlow();
        TopologyDesc topo=TopologyDesc.getInstance();
        ObjectMapper mapper=new ObjectMapper();
        try {
            JsonNode root=mapper.readTree(response);
            for(int i=0;i<Env.N_FLOWS;i++){
                JsonNode node=root.get("res" + String.valueOf(i+1));
                for(int k=0;k<node.size();k++){
                    JsonNode routing=node.get(k);
                    int start=routing.get(0).asInt();
                    int end=routing.get(routing.size()-1).asInt();
                    for(IpPrefix srcAddr:topo.getConnectedIps(topo.getDeviceId(start))){
                        for(IpPrefix dstAddr:topo.getConnectedIps(topo.getDeviceId(end))){
                            for(int j=0;j<routing.size()-1;j++){
                                int curr=routing.get(j).asInt();
                                int next=routing.get(j+1).asInt();
                                DeviceId currDeviceId=topo.getDeviceId(curr);
                                DeviceId nextHopDeviceId=topo.getDeviceId(next);
                                PortNumber output=topo.getConnectionPort(currDeviceId,nextHopDeviceId);
                                if(output.equals(INVALID_PORT)){
                                    continue;
                                    //todo log
                                }
                                FlowTableEntry entry=new FlowTableEntry();
                                entry.setDeviceId(currDeviceId)
                                        .setPriority(FlowEntryPriority.TABLE2_OPT_ROUTING)
                                        .setTable(2);
                                entry.filter()
                                        .setSrcIP(srcAddr)
                                        .setDstIP(dstAddr)
                                        .setVlanId(i);
                                entry.action()
                                        .setOutput(output);
                                FlowRule rule = entry.install(flowRuleService);
                                optiFlowRulesCache.add(rule);
                            }
                        }
                    }

                }
            }
            logger.info("---------connection down routing have been installed----------");
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    };

    SocketServerTask topoIdxServerTask;
    SocketServerTask.Handler topoIdxHandler = payload -> {
        logger.error("topo_idx info{}", payload);
        topoIdxJson = payload;
        waitTopoDiscover();
        TopologyDesc topo=TopologyDesc.getInstance();
        //update the port info
        topo.updateConnectionPort();
        //o change and shend message to algrithm  but don't handle the return data
        SocketClientTask.ResponseHandler responseH = res -> {
        };
        SocketClientTask task=new SocketClientTask(topoIdxJson + "*", responseH,App.DEFAULT_ROUTING_IP,
                App.DEFAULT_ROUTING_PORT);
        task.start();


        PeriodicalSocketClientTask defaultIdxRouteClientTask = new PeriodicalSocketClientTask(App.DEFAULT_ROUTING_IP,
                App.DEFAULT_ROUTING_PORT, defaultIdxRouteReq, defaultIdxRouteHandler);
        defaultIdxRouteClientTask.setOneTime(true).setDelay(10);
        defaultIdxRouteClientTask.start();
//        try {
//            Thread.sleep(15000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        List<Integer> asList = Arrays.asList(0, 1, 1, 0);
//        connectionDownReq(asList);
    };


    @Activate
    protected void activate() {
        logger.info("Activate started-------------");
        App.appId = coreService.registerApplication("org.chandler.smartfwd");
        Env.N_SWITCH=deviceService.getAvailableDeviceCount();
        logger.info("num:" + Env.N_SWITCH);

        logger.info("Populate cache");
        TopologyDesc.getInstance(deviceService,hostService,topologyService);
        logger.info("Populate done");
        logger.info("Init static flow table");
        init();
        //配置默认的一条路由
        installOnePath(new int[]{0,0,1,2,7,6,11,10,15,16,18,18}, "192.168.2.101/32");
        installOneBackPath(new int[]{18,18,16,15,10,11,6,7,2,1,0,0}, "192.168.2.101/32");

        installOnePath(new int[]{4,4,3,6,11,10,15,16,18,18}, "192.168.2.103/32");
        installOneBackPath(new int[]{18,18,16,15,10,11,6,3,4,4}, "192.168.2.103/32");

        installOnePath(new int[]{11,11,10,15,16,18,18}, "192.168.2.104/32");
        installOneBackPath(new int[]{18,18,16,15,10,11,11}, "192.168.2.104/32");

        //start flow entry install worker
        logger.info("Start Flow entry installation worker");
        flowEntryTask=new FlowEntryTask(flowEntries,flowRuleService);
        flowEntryTask.start();
        logger.info("Flow entry installation worker started");
//        //start flow classifier server;
//        logger.info("Start socket server for flow classifier");
//        classifierServer=new SocketServerTask(App.CLASSIFIER_LISTENING_IP, App.CLASSIFIER_LISTENING_PORT,classifierHandler);
//        classifierServer.start();s
//        logger.info("Socket server for flow classifier started");

        logger.info("Topo changed idx server start");
        topoIdxServerTask = new SocketServerTask(App.TOPO_IDX_IP, App.TOPO_IDX_PORT, topoIdxHandler);
        topoIdxServerTask.start();

//        //start traffic collector
        logger.info("Start traffic matrix collector");
        trafficMatrixCollector=new TrafficMatrixCollector(flowRuleService,TopologyDesc.getInstance(),trafficMatrixCollectorHandler);
        trafficMatrixCollector.start();
        logger.info("Traffic matrix collector started");

        logger.info("Start Port Rate Collector");
        portRateCollector=new PortStatsCollectTask(portStatisticsService,topologyService,portRateConsumer);
        portRateCollector.start();
        logger.info("Port Rate collector start");
        optiCondition();

        //链路qos多播测试
        String jsonfile = readFile("/data/telemetry.flow.json");
//        logger.info(jsonfile);
        handleTestData(jsonfile);
        installLinkTestFlow();

       /* try {
            TimeUnit.SECONDS.sleep(25);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("Attention: after 10s , some connection will be down!!!!!!");
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        connectionDownReq(Arrays.asList(0,1,1,0));
        logger.info("some connetions have down, and request for routing");*/

    }

    @Deactivate
    protected void deactivate() {
        App.getInstance().getPool().shutdownNow();
        App.getInstance().getScheduledPool().shutdownNow();
        flowEntryTask.stop();
//        classifierServer.stop();
        trafficMatrixCollector.stop();
        portRateCollector.stop();
        flowRuleService.removeFlowRulesById(App.appId);
        logger.info("--------------------System Stopped-------------------------");
    }


    /**
     * 初始化系统启动所需的相关方法.
     */
    void init() {
        //在table0 安装与host直连switch的默认路由
        installFlowEntryToEndSwitch();
        logger.info("Flow entry for end host installed");
//        //在table0 安装已经打标签的流的流表 将vlan5变为vland0
        installFlowEntryForLabelledTraffic();
        logger.info("Flow entry for labelled traffic installed");
//        //在table0 安装跳转到table2的流表
        installDefaultFlowEntryToTable0();
        logger.info("Flow entry for default traffic installed");
//        //安装table1 用于统计流量矩阵
        installFlowEntryForMatrixCollection();
        logger.info("Flow entry for traffic matrix collection installed");
//
//        //安装IP到table2的流表项
        installDefaultRoutingToTable2();
        installDropActionToTable2();
        logger.info("Flow entry for drop packet installed");


        //每隔1s统计一次接入端口流数据
//        storeFlowRateMission();
//        getMatrixMission();
    }

    //端口2152, udp流
    private void installOnePath(int[] routing, String IP){
        TopologyDesc topo=TopologyDesc.getInstance();
//        int[] routing = new int[]{0,0,11,10,9,21,33,34,35,35};
        for(int j=1;j<routing.length-1;j++){
            int pre = routing[j-1];
            int curr=routing[j];
            int next=routing[j+1];
            DeviceId preDeviceId = topo.getDeviceId(pre);
            DeviceId currDeviceId=topo.getDeviceId(curr);
            DeviceId nextHopDeviceId=topo.getDeviceId(next);
            PortNumber output=topo.getConnectionPort(currDeviceId,nextHopDeviceId);
            long inport = topo.getConnectionPort(currDeviceId, preDeviceId).toLong();
            if (inport == 0) {
                inport = 2;
            }
            if(output.equals(INVALID_PORT)){
                output = PortNumber.portNumber("2");
            }
            FlowTableEntry entry=new FlowTableEntry();
            entry.setDeviceId(currDeviceId)
                    .setPriority(FlowEntryPriority.ONE_ROUTING)
                    .setTable(0);
            entry.filter()
                    .setInport(inport)
                    .setSrcIP(IpPrefix.valueOf(IP));
            entry.action()
                    .setOutput(output);
            entry.install(flowRuleService);
        }
    }


    private void installOneBackPath(int[] routing, String IP){
        TopologyDesc topo=TopologyDesc.getInstance();
//        int[] routing = new int[]{0,0,11,10,9,21,33,34,35,35};
        for(int j=1;j<routing.length-1;j++){
            int pre = routing[j-1];
            int curr=routing[j];
            int next=routing[j+1];
            DeviceId preDeviceId = topo.getDeviceId(pre);
            DeviceId currDeviceId=topo.getDeviceId(curr);
            DeviceId nextHopDeviceId=topo.getDeviceId(next);
            PortNumber output=topo.getConnectionPort(currDeviceId,nextHopDeviceId);
            long inport = topo.getConnectionPort(currDeviceId, preDeviceId).toLong();
            if (inport == 0) {
                inport = 2;
            }
            if(output.equals(INVALID_PORT)){
                output = PortNumber.portNumber("2");
            }
            FlowTableEntry entry=new FlowTableEntry();
            entry.setDeviceId(currDeviceId)
                    .setPriority(FlowEntryPriority.ONE_ROUTING)
                    .setTable(0);
            entry.filter()
                    .setInport(inport)
                    .setDstIP(IpPrefix.valueOf(IP));
            entry.action()
                    .setOutput(output);
            entry.install(flowRuleService);
        }
    }


    public void installLinkTestFlow() {
        for(int i = 0; i < Env.N_SWITCH; i++)  {
            List<AflowEntry> aflowEntryList = flowMap.get(i);
            if (aflowEntryList == null) {
                return;
            }
            for(AflowEntry a : aflowEntryList) {
                installMultiCastFlow(a.srcIP, a.inport, a.outports, a.newSrcIP, a.newDstIP, a.vlan, String.valueOf(i));
            }
        }
    }

    private void installMultiCastFlow(String srcIP, String inport, List<String> outports, String newSrcIP, String newDstIP, String vlan, String sId) {
//        DeviceId deviceId = testSwMap.get(sId);
        DeviceId deviceId = TopologyDesc.getInstance().getDeviceId(sId);
        char c = srcIP.charAt(7);
        //table0 的流表下发
        if(c == '1') {
            DefaultFlowRule.Builder ruleBuilder = DefaultFlowRule.builder();
            TrafficSelector.Builder selectBuilder = DefaultTrafficSelector.builder();
            selectBuilder.matchEthType(Ethernet.TYPE_IPV4)
                    .matchInPort(PortNumber.portNumber(inport))
                    .matchIPSrc(IpPrefix.valueOf(srcIP))
                    .matchVlanId(VlanId.ANY);

            TrafficTreatment.Builder trafficBuilder = DefaultTrafficTreatment.builder();
            for(String output : outports) {
                if(output.equals("0")) {
                    logger.error("---port error--");
                }
                trafficBuilder.setOutput(PortNumber.portNumber(output));
            }
            ruleBuilder.withSelector(selectBuilder.build())
                    .withPriority(60010)
                    .withTreatment(trafficBuilder.build())
                    .forTable(0)
                    .fromApp(App.appId)
                    .makePermanent()
                    .forDevice(deviceId);
            FlowRuleOperations.Builder flowRulebuilder = FlowRuleOperations.builder();
            flowRulebuilder.add(ruleBuilder.build());
            flowRuleService.apply(flowRulebuilder.build());
        } else {

            DefaultFlowRule.Builder ruleBuilder0 = DefaultFlowRule.builder();
            TrafficSelector.Builder selectBuilder0 = DefaultTrafficSelector.builder();
            selectBuilder0.matchEthType(Ethernet.TYPE_IPV4)
                    .matchInPort(PortNumber.portNumber(inport))
                    .matchIPSrc(IpPrefix.valueOf(srcIP));

            TrafficTreatment.Builder trafficBuilder0 = DefaultTrafficTreatment.builder();
            for(String output : outports) {
                if(output.equals("0")) {
                    logger.error("---port error--");
                }
                trafficBuilder0.setOutput(PortNumber.portNumber(output));
            }
            if(!newSrcIP.equals("X")) {
                trafficBuilder0.transition(3);
                //处理返回包的流表项
                handleReturnPacket(srcIP, inport, newSrcIP, newDstIP, vlan, deviceId);
            }
            ruleBuilder0.withSelector(selectBuilder0.build())
                    .withPriority(60009)
                    .withTreatment(trafficBuilder0.build())
                    .forTable(0)
                    .fromApp(App.appId)
                    .makePermanent()
                    .forDevice(deviceId);
            FlowRuleOperations.Builder flowRulebuilder0 = FlowRuleOperations.builder();
            flowRulebuilder0.add(ruleBuilder0.build());
            flowRuleService.apply(flowRulebuilder0.build());
        }

    }

    private void handleReturnPacket(String srcIP, String inport, String newSrcIP, String newDstIP, String vlan,  DeviceId deviceId) {
        DefaultFlowRule.Builder ruleBuilder = DefaultFlowRule.builder();
        TrafficSelector.Builder selectBuilder = DefaultTrafficSelector.builder();
        selectBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(IpPrefix.valueOf(srcIP))
                .matchInPort(PortNumber.portNumber(inport))
                .matchVlanId(VlanId.ANY);

        TrafficTreatment.Builder trafficBuilder = DefaultTrafficTreatment.builder();
        trafficBuilder.setIpSrc(IpAddress.valueOf(newSrcIP))
                .setIpDst(IpAddress.valueOf(newDstIP))
                .setVlanId(VlanId.vlanId(Short.parseShort(vlan)))
                .setOutput(PortNumber.IN_PORT);

        ruleBuilder.withSelector(selectBuilder.build())
                .withPriority(60009)
                .withTreatment(trafficBuilder.build())
                .forTable(3)
                .fromApp(App.appId)
                .makePermanent()
                .forDevice(deviceId);
        FlowRuleOperations.Builder flowRulebuilder = FlowRuleOperations.builder();
        flowRulebuilder.add(ruleBuilder.build());
        flowRuleService.apply(flowRulebuilder.build());
    }

    private void handleTestData(String jsonData) {
        TopologyDesc instance = TopologyDesc.getInstance();
//        jsonData = jsonData.substring(0, jsonData.length() - 1);
        try {
            JsonNode jsonNode = new ObjectMapper().readTree(jsonData);
            for(int i = 0; i < Env.N_SWITCH; i++) {
                JsonNode deviceData = jsonNode.get(String.valueOf(i+1));
                Iterator<String> iterator = deviceData.fieldNames();
                List<AflowEntry> aflowEntryList = new ArrayList<>();
                while(iterator.hasNext()) {
                    String fieldName = iterator.next();
                    String[] split = fieldName.split(",");
                    String srcIP = split[0] + "/32";
                    String swithcId = String.valueOf(i);
                    int preId = Integer.parseInt(split[1]) - 1;
                    String inport = "";
                    if(preId == -1) {
                        inport = "1";
                    } else {
                        DeviceId cur = instance.getDeviceId(swithcId);
                        DeviceId pre = instance.getDeviceId(preId);
                        inport = instance.getConnectionPort(cur, pre).toString();
//                        inport = getPortInfo(testSwMap.get(swithcId), testSwMap.get(String.valueOf(preId))).toString();
                    }

                    JsonNode actions = deviceData.get(fieldName);
                    JsonNode action1 = actions.get("action1");
                    JsonNode action2 = actions.get("action2");
                    AflowEntry aflowEntry = null;
                    if(action1 != null && action2 != null) {
                        String outportText = action1.get("outport").toString();
                        outportText = outportText.substring(1, outportText.length() - 1);
                        String[] split1 = outportText.split(",");
                        List<String> outports = new ArrayList<>();
                        for(String dstId : split1) {
                            int outId = Integer.parseInt(dstId) - 1;
                            if(outId == -1) {
                                outports.add("1");
                            } else {
                                outports.add(instance.getConnectionPort(
                                        instance.getDeviceId(swithcId), instance.getDeviceId(outId)).toString());
//                                outports.add(getPortInfo(testSwMap.get(swithcId), testSwMap.get(String.valueOf(outId))).toString());
                            }
                        }
                        String newSrcIP = action2.get("src").asText();
                        String newDstIP = action2.get("dst").asText();
                        String vlan = action2.get("vlan").asText();
                        aflowEntry = new AflowEntry(srcIP, inport, outports, newSrcIP, newDstIP, vlan);
                    } else if(action1 != null) {
                        String outportText = action1.get("outport").toString();
                        outportText = outportText.substring(1, outportText.length() - 1);
                        String[] split1 = outportText.split(",");
                        List<String> outports = new ArrayList<>();
                        for(String dstId : split1) {
                            int outId = Integer.parseInt(dstId) - 1;
                            if(outId == -1) {
                                outports.add("1");
                            } else {
                                outports.add(instance.getConnectionPort(
                                        instance.getDeviceId(swithcId), instance.getDeviceId(outId)).toString());
//                                outports.add(getPortInfo(testSwMap.get(swithcId), testSwMap.get(String.valueOf(outId))).toString());
                            }
                        }
                        aflowEntry = new AflowEntry(srcIP, inport, outports, "X", "X", "-1");
                    } else if(action2 != null) {
                        List<String> outports = new ArrayList<>();
                        String newSrcIP = action2.get("src").asText();
                        String newDstIP = action2.get("dst").asText();
                        String vlan = action2.get("vlan").asText();
                        aflowEntry = new AflowEntry(srcIP, inport, outports, newSrcIP, newDstIP, vlan);
                    }
                    aflowEntryList.add(aflowEntry);
                }
                flowMap.put(i, aflowEntryList);
            }

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            logger.info(e.toString());
        }
    }


    void installFlowEntryToEndSwitch() {
        for(Device device:deviceService.getAvailableDevices()){
            for(Host host:hostService.getConnectedHosts(device.id())) {
                for(IpAddress ipAddr:host.ipAddresses()){
                    PortNumber output=host.location().port();
                    FlowTableEntry entry=new FlowTableEntry();
                    entry.setTable(0)
                            .setPriority(FlowEntryPriority.TABLE0_HANDLE_LAST_HOP)
                            .setDeviceId(device.id());
                    entry.filter()
                            .setDstIP(ipAddr.toIpPrefix());
                    entry.action()
                            .setOutput(output);
                    entry.install(flowRuleService);
                }
            }
        }
        logger.debug("Host connection flow installed");
    }

    void installFlowEntryForLabelledTraffic(){
        for(Device device:deviceService.getAvailableDevices()){
            Set<Host> hosts=hostService.getConnectedHosts(device.id());
            for(int vlanId = 5; vlanId< 6; vlanId++){
                FlowTableEntry entry=new FlowTableEntry();
                entry.setPriority(FlowEntryPriority.TABLE0_HANDLE_TAGGED_FLOW)
                        .setTable(0)
                        .setDeviceId(device.id());
                entry.filter()
                        .setVlanId(vlanId);
                entry.action()
                        .setVlanId(0)
                        .setTransition(1);
                for(Host host:hosts){
                    for(IpAddress addr:host.ipAddresses()){
                        entry.filter()
                                .setSrcIP(addr.toIpPrefix());
                        entry.install(flowRuleService);
                    }
                }

            }
        }
    }
    void installDefaultFlowEntryToTable0(){
        for(Device device:deviceService.getAvailableDevices()){
            FlowTableEntry entry=new FlowTableEntry();
            entry.setTable(0)
                    .setPriority(FlowEntryPriority.TABLE0_DEFAULT)
                    .setDeviceId(device.id());
            entry.action()
                    .setTransition(2);
            entry.install(flowRuleService);
        }
    }

    void installDefaultRoutingToTable2(){
        TopologyDesc topo=TopologyDesc.getInstance();
        String req="{ \"topo_idx\" : 0}*";
        SocketClientTask.ResponseHandler responseHandler = payload -> {
//            logger.info(payload);
            try{
                ObjectMapper mapper=new ObjectMapper();
                JsonNode root=mapper.readTree(payload);
                JsonNode routings=root.get("res1");
                for(int i=0;i<routings.size();i++){
                    JsonNode routing=routings.get(i);
//                    logger.info(routing.toString());
                    int start=routing.get(0).asInt();
                    int end=routing.get(routing.size()-1).asInt();
                    for(IpPrefix srcAddr:topo.getConnectedIps(topo.getDeviceId(start))){
                        for(IpPrefix dstAddr:topo.getConnectedIps(topo.getDeviceId(end))){
                            for(int j=0;j<routing.size()-1;j++){
                                int curr=routing.get(j).asInt();
                                int next=routing.get(j+1).asInt();
                                DeviceId currDeviceId=topo.getDeviceId(curr);
                                DeviceId nextHopDeviceId=topo.getDeviceId(next);
                                PortNumber output=topo.getConnectionPort(currDeviceId,nextHopDeviceId);
                                if(output.equals(INVALID_PORT)){
                                    logger.info("invalid port  " + routing.toString());
                                    continue;
                                    //todo log
                                }
                                FlowTableEntry entry=new FlowTableEntry();
                                entry.setDeviceId(currDeviceId)
                                        .setPriority(FlowEntryPriority.TABLE2_DEFAULT_ROUTING)
                                        .setTable(2);
                                entry.filter()
                                        .setSrcIP(srcAddr)
                                        .setDstIP(dstAddr);
                                entry.action()
                                        .setOutput(output);
                                FlowRule rule = entry.install(flowRuleService);
                                defaultFlowRulesCache.add(rule);
                            }
                        }
                    }


                }

                logger.info("install success");
            }
            catch (JsonProcessingException e) {
                e.printStackTrace();
                logger.info(e.toString());
            }
        };
        SocketClientTask task=new SocketClientTask(req, responseHandler,App.DEFAULT_ROUTING_IP,App.DEFAULT_ROUTING_PORT);
        task.start();
    }
    void installDropActionToTable2(){
        for(Device device:deviceService.getAvailableDevices()){
            FlowTableEntry entry=new FlowTableEntry();
            entry.setDeviceId(device.id())
                    .setPriority(FlowEntryPriority.TABLE2_DROP)
                    .setTable(2);
            entry.action()
                    .setDrop(true);
            entry.install(flowRuleService);
        }
    }

    /**
     * 清空默认流表项.
     */
    void emptyDefaultFlow() {
        if (defaultFlowRulesCache.size() != 0) {
            for (FlowRule flowRule : defaultFlowRulesCache) {
                flowRuleService.removeFlowRules(flowRule);
            }
            // 清空数据
            defaultFlowRulesCache.clear();
        }
        logger.info("---------have emptyed the default flowEntries----------------");
    }

    /**
     * 清空优化流表项.
     */
    void emptyOptiFlow() {
        if (optiFlowRulesCache.size() != 0) {
            for (FlowRule flowRule : optiFlowRulesCache) {
                flowRuleService.removeFlowRules(flowRule);
            }
            // 清空数据
            optiFlowRulesCache.clear();
        }
        logger.info("---------have emptyed the optimize flowEntries----------------");
    }

    public Double getCurrentMaxRate() {
        long max = 0L;
        Map<SwitchPair, Long> map = portRate.get();
        if(null == map) {
            return 0.0;
        }
        for(long rate : map.values()) {
            max = Math.max(max, rate);
        }
        return max * 8 / 1000000.0;
    }

    public String getAllRate() {
        ArrayList<Double> res = new ArrayList<>();
        Map<SwitchPair, Long> map = portRate.get();
        if(null == map) {
            return "";
        }
        for(long rate : map.values()) {
            res.add(rate * 8/ 1000000.0);
        }
        Collections.sort(res);
        return res.toString();
    }

    public void optiCondition() {
       /* PeriodicalSocketClientTask optRoutingRequestTask = new PeriodicalSocketClientTask(App.OPT_ROUTING_IP,
                App.OPT_ROUTING_PORT, optRoutingReqGenerator, optRoutingRespHandler);
        optRoutingRequestTask.setDelay(120).setInterval(200);
        optRoutingRequestTask.start();*/
        OptiConditionTask conditionTask = new OptiConditionTask(portRate, optRoutingReqGenerator, optRoutingRespHandler, 150);
        conditionTask.setInterval(10).start();
    }

    public void testCommand() {
        logger.info("just test the shell command, yeah!");
    }


    /**
     * 模拟链路断掉，将断掉链路信息传输给算法模块，并下发新的路由.
     * @param downList
     */
    public void connectionDownReq(List<Integer> downList) {
        JsonObject sendInfo = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        for(int i : downList) {
            jsonArray.add(i);
        }
        sendInfo.set("disconnectEdge", jsonArray);
        ObjectMapper mapper=new ObjectMapper();
        try {
            JsonNode topoIdx = mapper.readTree(topoIdxJson);
            int topo_idx = topoIdx.get("topo_idx").asInt();
            sendInfo.set("topo_idx", topo_idx);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        String payload = sendInfo.toString() + "*";
        SocketClientTask connectionDownTask = new SocketClientTask(payload, connectionDownHandler,
                App.C_DOWN_ROUTING_IP, App.C_DOWN_ROUTING_PORT);
        connectionDownTask.start();
    }

    /**
     * 一直等待topo发现完全.
     */
    void waitTopoDiscover() {
        int topoId = 0;
        int linksCount = 0;
        logger.info("---------------discover topo waiting.....-----------------------");
        while (true) {
            linksCount = topologyService.currentTopology().linkCount();
            try {
                JsonNode jsonNode = new ObjectMapper().readTree(topoIdxJson);
                topoId = jsonNode.get("topo_idx").intValue();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (topoId % 2 == 0) {
                if (linksCount == 202) {
                    break;
                }
            } else {
                if (linksCount == 212) {
                    break;
                }
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        logger.info("--------topo discover complete------------");
    }

    public void rateToFile(AtomicReference<Map<SwitchPair,Long>> portRate, String path) {
        Map<SwitchPair, Long> map = portRate.get();
        if(null == map) {
            return;
        }
        Set<SwitchPair> switchPairs = map.keySet();
        for(SwitchPair switchPair : switchPairs) {
            DeviceId src = switchPair.src;
            DeviceId dst = switchPair.dst;
            TopologyDesc topologyDesc = TopologyDesc.getInstance();
            Integer srcId = topologyDesc.deviceIDToSwitchID.get(src);
            Integer dstId = topologyDesc.deviceIDToSwitchID.get(dst);
            Long aLong = map.get(switchPair);
            String out = srcId + " " + dstId + " " + aLong;
            writeToFile(out, path);
        }
    }

    /**
     * 把信息输出到文件.
     * @param content
     */
    public void writeToFile(String content, String filePath) {
        try {
            File file = new File(filePath);
            if(!file.exists()){
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file.getAbsoluteFile(), true);
            BufferedWriter bw = new BufferedWriter(fileWriter);
            /*SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String format = df.format(new Date());
            String out = format + "-->>" + content + "\n";
            bw.write(out);*/
            bw.write(content + "\n");
            bw.close();
//            log.info("finished write to file");
        } catch (IOException e) {
            logger.error(e.toString());
        }
    }

    public String readFile(String path) {
        BufferedReader reader = null;
        String laststr = "";
        try {
            FileInputStream fileInputStream = new FileInputStream(path);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
            reader = new BufferedReader(inputStreamReader);
            String tempString = null;
            while ((tempString = reader.readLine()) != null) {
                laststr += tempString;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return laststr;
    }

    public void writeTimeHeaderToFile(String filePath) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String format = df.format(new Date());
        writeToFile(format, filePath);
    }

    private void installFlowEntryForMatrixCollection() {
        for(Host srcHost:TopologyDesc.getInstance().getHosts()){
            DeviceId switchID=srcHost.location().deviceId();
            for(Host dstHost:TopologyDesc.getInstance().getHosts()){
                if(!srcHost.equals(dstHost)){
                    for(IpAddress srcIp:srcHost.ipAddresses()){
                        for(IpAddress dstIp:dstHost.ipAddresses()){
                            for(int vlanId=0;vlanId<Env.N_FLOWS;vlanId++){
                                FlowTableEntry entry=new FlowTableEntry();
                                entry.setDeviceId(switchID)
                                        .setTable(1)
                                        .setPriority(FlowEntryPriority.TABLE1_MATRIX_COLLECTION);
                                entry.filter()
                                        .setSrcIP(srcIp.toIpPrefix())
                                        .setDstIP(dstIp.toIpPrefix())
                                        .setVlanId(vlanId);
                                entry.action()
                                        .setTransition(2);
                                entry.install(flowRuleService);
                            }
                        }
                    }
                }
            }
        }
    }

    class OptiConditionTask extends PeriodicalTask {
        AtomicReference<Map<SwitchPair,Long>> portRate;
        long preReqTime = 0L;
        int interval;
//    PeriodicalSocketClientTask.RequestGenerator optRoutingReqGenerator;
//    PeriodicalSocketClientTask.ResponseHandler optRoutingRespHandler;

        public OptiConditionTask(AtomicReference<Map<SwitchPair,Long>>  rates,
                                 PeriodicalSocketClientTask.RequestGenerator optRoutingReqGenerator,
                                 PeriodicalSocketClientTask.ResponseHandler optRoutingRespHandler,
                                 int interval) {
            this.portRate = rates;
            this.interval = interval;
//        this.optRoutingReqGenerator = optRoutingReqGenerator;
//        this.optRoutingRespHandler = optRoutingRespHandler;
            this.worker = () -> {
                ArrayList<Double> res = new ArrayList<>();
                Map<SwitchPair, Long> map = this.portRate.get();
                if(null == map) {
                    return;
                }
                for(long rate : map.values()) {
                    res.add(rate * 8/ 1000000.0);
                }
                int cntBits = 0;
                for(Double rate : res) {
                    if(rate > 48.0) {
                        cntBits++;
                    }
                }
                long nowReqTime = new Date().getTime();
                long intervalTime = nowReqTime - preReqTime;
                if(cntBits >= 2) {
                    logger.info("cntBits:--->" + cntBits);
                    if(intervalTime >= this.interval * 1000) {
                        logger.info("<<<<<<----------------request the opt routing----------------->>>>>>>>>>");
//                        long time = new Date().getTime();
//                        rateFilePath = "/data/" + String.valueOf(time) + ".rate";
//                        writeToFile("pre", rateFilePath);
//                        rateToFile(portRate, rateFilePath);

                        PeriodicalSocketClientTask optRoutingRequestTask = new PeriodicalSocketClientTask(App.OPT_ROUTING_IP,
                                App.OPT_ROUTING_PORT, optRoutingReqGenerator, optRoutingRespHandler);
                        optRoutingRequestTask.setOneTime(true).setDelay(0);
                        optRoutingRequestTask.start();
                        preReqTime = nowReqTime;
                    }

                }

            };
        }
    }

    /**
     * 每一条流表项的类.
     */
    public class AflowEntry {
        String srcIP;
        String inport;
        List<String> outports;
        String newSrcIP;
        String newDstIP;
        String vlan;
        public AflowEntry(String srcIP, String inport, List<String> outports, String newSrcIP, String newDstIp, String vlan) {
            this.srcIP = srcIP;
            this.inport = inport;
            this.outports = outports;
            this.newSrcIP = newSrcIP;
            this.newDstIP = newDstIp;
            this.vlan = vlan;
        }

        @Override
        public String toString() {
            return "AflowEntry{" +
                    "srcIP='" + srcIP + '\'' +
                    ", inport='" + inport + '\'' +
                    ", outports=" + outports +
                    ", newSrcIP='" + newSrcIP + '\'' +
                    ", newDstIP='" + newDstIP + '\'' +
                    ", vlan=" + vlan +
                    '}';
        }
    }

}




