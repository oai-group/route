package apps.smartfwd.src.main.java.task.base;

import apps.smartfwd.src.main.java.constants.App;
import apps.smartfwd.src.main.java.task.base.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public  abstract class AbstractStoppableTask implements StoppableTask {
    protected final Logger logger= LoggerFactory.getLogger(getClass().getName());
    protected AtomicBoolean isRunning=new AtomicBoolean();
    protected ExecutorService service= App.getInstance().getPool();
    protected Thread worker;
    protected boolean stopRequested=false;
    protected Future<?> handle;

    public void start(){
        if(null==service){
            worker=new Thread(this);
            worker.start();
        }else{
            handle=service.submit(this);
        }
    }
    public void stop(){
        if(null==service){
            worker.interrupt();
        }else{
            if(null!=handle){
                handle.cancel(true);
            }
        }
    }
    public boolean isRunning(){
        return isRunning.get();
    }
}
