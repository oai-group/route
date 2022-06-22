package apps.smartfwd.src.main.java.models;

import apps.smartfwd.src.main.java.TopologyDesc;
import org.onosproject.net.DeviceId;

import java.util.HashMap;
import java.util.Map;

public class SwitchPair{
    public final DeviceId src;
    public final DeviceId dst;
    static TopologyDesc desc=TopologyDesc.getInstance();
    static Map<String,SwitchPair> cache=new HashMap<>();
    private SwitchPair(DeviceId s,DeviceId d){
        this.src=s;
        this.dst=d;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SwitchPair)) return false;
        SwitchPair key = (SwitchPair) o;
        return src == key.src && dst == key.dst;
    }

    @Override
    public int hashCode() {
        return (src.hashCode()<<16)+dst.hashCode();
    }
    public String toString(){
        return src.toString()+"-"+dst.toString();
    }
    public static SwitchPair switchPair(DeviceId s,DeviceId d){
        String key=s.toString()+"-"+d.toString();
        if(!cache.containsKey(key)){
            cache.put(key,new SwitchPair(s,d));
        }
        return cache.get(key);
    }
//    public static SwitchPair switchPair(int s,int d){
//        return switchPair(desc.getDeviceId(s),desc.getDeviceId(d));
//    }
}
