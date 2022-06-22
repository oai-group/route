package apps.smartfwd.src.main.java;

import apps.smartfwd.src.main.java.constants.App;
import org.onlab.packet.*;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.*;

 class Filter{
    IpPrefix srcIP;
    IpPrefix dstIP;
    byte protocol=-1;
    int sport=-1;
    int dport=-1;
    int vlanId=-1;
    long inport=-1;


    public long getInport() {
        return this.inport;
    }
    public Filter setInport(long inport) {
        this.inport = inport;
        return this;
    }


    public IpPrefix getSrcIP() {
        return srcIP;
    }

    public Filter setSrcIP(IpPrefix srcIP) {
        this.srcIP = srcIP;
        return this;
    }

    public IpPrefix getDstIP() {
        return dstIP;
    }

    public Filter setDstIP(IpPrefix dstIP) {
        this.dstIP = dstIP;
        return this;
    }

    public byte getProtocol() {
        return protocol;
    }

    public Filter setProtocol(byte protocol) {
        this.protocol = protocol;
        return this;
    }

    public int getSport() {
        return sport;
    }

    public Filter setSport(int sport) {
        this.sport = sport;
        return this;
    }

    public int getDstPort() {
        return dport;
    }

    public Filter setDstPort(int dport) {
        this.dport = dport;
        return this;
    }

    public int getVlanId() {
        return vlanId;
    }

    public Filter setVlanId(int vlanId) {
        this.vlanId = vlanId;
        return this;
    }
}

class Action{
    int transition=-1;
    PortNumber output;
    int vlanId=-1;
    boolean drop=false;

    public int getTransition() {
        return transition;
    }

    public int getVlanId() {
        return vlanId;
    }

    public Action setVlanId(int vlanId) {
        this.vlanId = vlanId;
        return this;
    }

    public Action setTransition(int transition) {
        this.transition = transition;
        return this;
    }

    public PortNumber getOutput() {
        return output;
    }

    public Action setOutput(PortNumber output) {
        this.output = output;
        return this;
    }

    public boolean isDrop() {
        return drop;
    }

    public Action setDrop(boolean drop) {
        this.drop = drop;
        return this;
    }
}
public class FlowTableEntry {
    int table=0;
    int priority=1;
    int timeout;
    DeviceId deviceId;

    public int getTable() {
        return table;
    }

    public FlowTableEntry setTable(int table) {
        this.table = table;
        return this;
    }

    public int getPriority() {
        return priority;
    }

    public FlowTableEntry setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public int getTimeout() {
        return timeout;
    }

    public FlowTableEntry setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public DeviceId getDeviceId() {
        return deviceId;
    }

    public FlowTableEntry setDeviceId(DeviceId deviceId) {
        this.deviceId = deviceId;
        return this;
    }

    Filter _filter=new Filter();
    Action _action=new Action();

    public Filter filter(){
        return _filter;
    }

    public Action action(){
        return _action;
    }

    //todo
    public boolean check(){
        return true;
    }

    public FlowRule install(FlowRuleService service){
        DefaultFlowRule.Builder ruleBuilder=DefaultFlowRule.builder();
        TrafficSelector.Builder selectorBuilder= DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4);
        if(!check()){
            return null;
        }

        if(-1!=_filter.inport){
            selectorBuilder.matchInPort(PortNumber.portNumber(_filter.inport));
        }

        if(null!=_filter.srcIP){
            selectorBuilder.matchIPSrc(_filter.srcIP);
                   
        }
        if(null!=_filter.dstIP){
            selectorBuilder.matchIPDst(_filter.dstIP);
        }
        if(-1!=_filter.vlanId){
            selectorBuilder.matchVlanId(VlanId.vlanId((short)_filter.vlanId));
        }
        if(-1!=_filter.protocol){
            selectorBuilder.matchIPProtocol(_filter.protocol);
        }

        if(-1!=_filter.sport) {
            if(IPv4.PROTOCOL_TCP==_filter.protocol){
                selectorBuilder.matchTcpSrc(TpPort.tpPort(_filter.sport));
            }else{
                selectorBuilder.matchUdpSrc(TpPort.tpPort(_filter.sport));
            }
        }
        
        if(-1!=_filter.dport){
            if(IPv4.PROTOCOL_TCP==_filter.protocol){
                selectorBuilder.matchTcpDst(TpPort.tpPort(_filter.dport));
            }else{
                selectorBuilder.matchUdpDst(TpPort.tpPort(_filter.dport));
            } 
        }

        TrafficTreatment.Builder trafficBuilder=DefaultTrafficTreatment.builder();
        if(!_action.drop){
            //set vlanid
            if(-1!=_action.vlanId){
                trafficBuilder.setVlanId(VlanId.vlanId((short) _action.vlanId));
            }
            //set output
            if(null!=_action.output){
                trafficBuilder.setOutput(_action.output);
            }
            //set transition
            if(-1!=_action.transition){
                trafficBuilder.transition(_action.transition);
            }
        }else{
            trafficBuilder.drop();
        }

        ruleBuilder.withSelector(selectorBuilder.build())
                .withTreatment(trafficBuilder.build())
                .withPriority(priority)
                .forTable(table)
                .fromApp(App.appId);
        ruleBuilder.forDevice(deviceId);
        if(-1!=timeout){
            ruleBuilder.withIdleTimeout(timeout);
        }else{
            ruleBuilder.makePermanent();
        }
//        DefaultForwardingObjective.Builder builder=DefaultForwardingObjective.builder();
//        builder.withSelector(selectorBuilder.build())
//                .withTreatment(trafficBuilder.build())
//                .withPriority(priority)
//                .for
        FlowRuleOperations.Builder flowRuleOpBuilder=FlowRuleOperations.builder();
        FlowRule build = ruleBuilder.build();
        flowRuleOpBuilder.add(build);
        service.apply(flowRuleOpBuilder.build());
        return build;

    }

}
