package apps.smartfwd.src.main.java.task.base;


import apps.smartfwd.src.main.java.constants.App;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class PeriodicalTask extends AbstractStoppableTask{

    public interface Worker{
        void doWork();
    }
    protected ScheduledExecutorService scheduledPool = App.getInstance().getScheduledPool();
    //seconds
    int interval=7;
    int delay=5;
    boolean oneTime = false;
    protected ScheduledFuture<?> handle;
    protected Worker worker;

    public int getInterval() {
        return interval;
    }

    public PeriodicalTask setInterval(int interval) {
        this.interval = interval;
        return this;
    }

    public int getDelay() {
        return delay;
    }

    public PeriodicalTask setDelay(int delay) {
        this.delay = delay;
        return this;
    }

    public PeriodicalTask setOneTime(boolean t) {
        this.oneTime = t;
        return this;
    }

    public void run(){
        try{
                worker.doWork();
        }catch (Exception e){
            //logger.info(e.getLocalizedMessage());
            logger.info(getStackTrace(e));
//            logger.info(e.getMessage());
//            e.printStackTrace();
        }
    }
    public void start(){
        if(!isRunning.get()){
            if(oneTime) {
                handle= scheduledPool.schedule(this, this.delay, TimeUnit.SECONDS);
            } else {
                handle= scheduledPool.scheduleAtFixedRate(this,delay, this.interval,TimeUnit.SECONDS);
            }

        }
    }
    public void stop(){
        if(isRunning.get()){
            handle.cancel(true);
        }
        isRunning.set(false);
    }
    public boolean isRunning(){
        return isRunning.get();
    }

    private static String getStackTrace(Exception ex) {
        StringBuffer sb = new StringBuffer(500);
        StackTraceElement[] st = ex.getStackTrace();
        sb.append(ex.getClass().getName() + ": " + ex.getMessage() + "\n");
        for (int i = 0; i < st.length; i++) {
            sb.append("\t at " + st[i].toString() + "\n");
        }
        return sb.toString();
    }
}

