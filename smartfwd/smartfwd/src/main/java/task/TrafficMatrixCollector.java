package apps.smartfwd.src.main.java.task;

import apps.smartfwd.src.main.java.TopologyDesc;
import apps.smartfwd.src.main.java.constants.FlowEntryPriority;
import apps.smartfwd.src.main.java.models.SwitchPair;
import apps.smartfwd.src.main.java.constants.Env;
import apps.smartfwd.src.main.java.task.base.PeriodicalTask;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.*;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrafficMatrixCollector extends PeriodicalTask {
    public interface Handler{
        void handle(List<Map<SwitchPair,Long>> stats);
    }

    TopologyDesc desc;
    Handler handler;
    List<Map<SwitchPair,Long>> preStats=new ArrayList<>();
    List<Map<SwitchPair,Long>> currStats=new ArrayList<>();
    FlowRuleService flowRuleService;
    private final Logger logger= LoggerFactory.getLogger(getClass().getName());
    public TrafficMatrixCollector(FlowRuleService flowRuleService, TopologyDesc topo, Handler handler){
        this.desc=topo;
        this.handler=handler;
        this.flowRuleService=flowRuleService;
        //init
        for(int i = 0; i< Env.N_FLOWS+1; i++){
            currStats.add(new HashMap<>());
            preStats.add(new HashMap<>());
        }

        this.worker=()->{
            //init
            List<Map<SwitchPair,Long>> res=new ArrayList<>();
            for(int i=0;i<Env.N_FLOWS+1;i++){
                res.add(new HashMap<>());
            }
            //reset
            currStats.forEach(stats-> stats.replaceAll((k, v) -> 0L));

            for(DeviceId srcId:topo.getDeviceIds()){
                for(FlowEntry entry:flowRuleService.getFlowEntries(srcId)){
                    if(!entry.table().equals(IndexTableId.of(1))&&!entry.table().equals(IndexTableId.of(2))) continue;
                    if(entry.priority()!= FlowEntryPriority.TABLE1_MATRIX_COLLECTION&&entry.priority()!=FlowEntryPriority.TABLE2_DEFAULT_ROUTING) continue;

                    TrafficSelector selector=entry.selector();
                    IPCriterion dstCriterion=(IPCriterion) selector.getCriterion(Criterion.Type.IPV4_DST);
                    IPCriterion srcCriterion=(IPCriterion) selector.getCriterion(Criterion.Type.IPV4_SRC);
                    DeviceId dstId=topo.getConnectedDeviceFromIp(dstCriterion.ip());
                    DeviceId sId=topo.getConnectedDeviceFromIp(srcCriterion.ip());
                    if(!sId.equals(srcId)) continue;
                    SwitchPair key=SwitchPair.switchPair(srcId,dstId);

                    if(entry.table().equals(IndexTableId.of(1))){
                        VlanIdCriterion vlanIdCriterion=(VlanIdCriterion)selector.getCriterion(Criterion.Type.VLAN_VID);
                        int vlanId=vlanIdCriterion.vlanId().toShort();
                        Map<SwitchPair,Long> stats=currStats.get(vlanId);
                        stats.put(key,stats.getOrDefault(key,0L)+entry.bytes());
                    }else{
                        Map<SwitchPair,Long> stats=currStats.get(Env.N_FLOWS);
                        stats.put(key,stats.getOrDefault(key,0L)+entry.bytes());
                    }

                }
            }

            //diff and replace
            for(int i = 0; i< Env.N_FLOWS+1; i++){
                Map<SwitchPair,Long> curr=currStats.get(i);
                Map<SwitchPair,Long> pre=preStats.get(i);
                Map<SwitchPair,Long> r=new HashMap<>();
                //diff and replace
                for(SwitchPair key:curr.keySet()){
                    r.put(key,curr.get(key)-pre.getOrDefault(key,0L));
                    pre.put(key,curr.get(key));
                }
                res.set(i,r);
            }
            handler.handle(res);
        };
    }

}
