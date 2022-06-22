package apps.smartfwd.src.main.java.constants;

import org.onosproject.core.ApplicationId;

import javax.xml.crypto.dsig.spec.XSLTTransformParameterSpec;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class App {
    public static ApplicationId appId;
    ExecutorService pool= Executors.newCachedThreadPool();
    ScheduledExecutorService scheduledPool=Executors.newScheduledThreadPool(10);
    static App instance=new App();
    private App(){}
    public static App getInstance(){
        return instance;
    }
    public ExecutorService getPool(){
        return pool;
    }
    public ScheduledExecutorService getScheduledPool(){
        return scheduledPool;
    }
    public static final String CLASSIFIER_LISTENING_IP="0.0.0.0";
    public static final int CLASSIFIER_LISTENING_PORT=1050;

    public static final String TOPO_IDX_IP = "0.0.0.0";
    public static final int TOPO_IDX_PORT = 1051;

    public static final String ALG_CLASSIFIER_IP="algorithm_instance";
    public static final int ALG_CLASSIFIER_PORT=1052;

    public static final String DEFAULT_ROUTING_IP="algorithm_instance";
    public static final int DEFAULT_ROUTING_PORT=1053;

    public static final String OPT_ROUTING_IP="algorithm_instance";
    public static final int OPT_ROUTING_PORT=1053;
//    public static final String OPT_ROUTING_IP="192.168.1.90";
//    public static final int OPT_ROUTING_PORT=1055;

    public static final String C_DOWN_ROUTING_IP="algorithm_instance";
    public static final int C_DOWN_ROUTING_PORT=1053;

}

